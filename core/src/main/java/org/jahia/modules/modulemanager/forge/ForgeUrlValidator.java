package org.jahia.modules.modulemanager.forge;

import org.apache.commons.lang.StringUtils;
import org.jahia.settings.SettingsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Decides whether a forge base URL is one the module store may fetch from. A forge URL is fetched
 * server-side (its module list is loaded at configuration time and on every reload), so the store
 * needs it to name a routable endpoint it is meant to reach. This accepts an absolute http(s) URL
 * whose host resolves only to routable addresses, and refuses everything else: a non-absolute or
 * non-http(s) URL, a URL with no host, and a host that resolves to a loopback, link-local,
 * wildcard, multicast or private-range address.
 *
 * <p>An operator that genuinely runs a forge on an internal host opts it back in by listing its
 * host in the {@value #ALLOWED_INTERNAL_HOSTS_PROPERTY} Jahia property (comma-separated, empty by
 * default), so the default is safe and the internal-forge deployment is still expressible.
 */
public final class ForgeUrlValidator {

    /**
     * Jahia property naming the hosts whose non-routable address is nonetheless accepted, so an
     * operator running an internal private app store can keep using it. Comma-separated, empty by
     * default.
     */
    public static final String ALLOWED_INTERNAL_HOSTS_PROPERTY = "org.jahia.modules.modulemanager.forge.allowedInternalHosts";

    private static final Logger logger = LoggerFactory.getLogger(ForgeUrlValidator.class);
    private static final Set<String> ALLOWED_SCHEMES = new HashSet<>(Arrays.asList("http", "https"));

    private ForgeUrlValidator() {
    }

    /**
     * Validates {@code url}, reading the operator allow-list from the current Jahia settings.
     *
     * @param url the forge base URL
     * @return true when the module store may fetch from this URL
     */
    public static boolean isAllowed(String url) {
        return isAllowed(url, allowedInternalHosts());
    }

    /**
     * Validates {@code url} against an explicit allow-list, so the decision can be exercised without
     * a running Jahia.
     *
     * @param url                  the forge base URL
     * @param allowedInternalHosts hosts (lower-cased) the operator has opted back in; may be empty
     * @return true when the URL is an absolute http(s) URL whose host is routable or explicitly allowed
     */
    public static boolean isAllowed(String url, Set<String> allowedInternalHosts) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            return false;
        }
        String host = uri.getHost();
        if (StringUtils.isBlank(host)) {
            // opaque URI, missing authority, or an authority a strict parser would not accept as a host
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (allowedInternalHosts != null && allowedInternalHosts.contains(normalizedHost)) {
            return true;
        }
        String lookup = host;
        if (lookup.length() > 1 && lookup.charAt(0) == '[' && lookup.charAt(lookup.length() - 1) == ']') {
            lookup = lookup.substring(1, lookup.length() - 1);   // an IPv6 literal reaches here bracketed
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(lookup)) {
                if (isBlockedAddress(address)) {
                    // a host is accepted only when every address it resolves to is routable
                    return false;
                }
            }
        } catch (UnknownHostException e) {
            return false;
        }
        return true;
    }

    /**
     * @return true for a loopback, link-local, site-local (RFC 1918), wildcard/any-local or
     * multicast address — the ranges a routable forge endpoint is not expected to use.
     */
    static boolean isBlockedAddress(InetAddress address) {
        return address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isAnyLocalAddress()
                || address.isMulticastAddress();
    }

    private static Set<String> allowedInternalHosts() {
        try {
            String raw = SettingsBean.getInstance().getPropertiesFile().getProperty(ALLOWED_INTERNAL_HOSTS_PROPERTY);
            if (StringUtils.isBlank(raw)) {
                return Collections.emptySet();
            }
            Set<String> hosts = new HashSet<>();
            for (String host : StringUtils.split(raw, ",")) {
                if (StringUtils.isNotBlank(host)) {
                    hosts.add(host.trim().toLowerCase(Locale.ROOT));
                }
            }
            return hosts;
        } catch (Exception e) {
            logger.warn("Unable to read {}; treating the forge internal-host allow-list as empty", ALLOWED_INTERNAL_HOSTS_PROPERTY, e);
            return Collections.emptySet();
        }
    }
}

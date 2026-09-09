package org.jahia.modules.modulemanager.forge;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Truth table for {@link ForgeUrlValidator}. Every case uses an IP literal or a syntactic input so
 * the decision is deterministic and needs no name resolution: {@code InetAddress.getAllByName} parses
 * a literal without a DNS lookup. Both directions are asserted, so a validator hard-wired to accept
 * or to refuse everything fails the suite.
 */
public class ForgeUrlValidatorTest {

    private static final Set<String> NONE = Collections.emptySet();

    // --- accepted: a routable http(s) endpoint is what the store legitimately needs -------------

    @Test
    public void acceptsARoutablePublicHttpsUrl() {
        assertTrue(ForgeUrlValidator.isAllowed("https://93.184.216.34/contents/modules-repository.moduleList.json", NONE));
    }

    @Test
    public void acceptsARoutablePublicHttpUrl() {
        assertTrue(ForgeUrlValidator.isAllowed("http://8.8.8.8/store", NONE));
    }

    @Test
    public void acceptsARoutablePublicIpv6Url() {
        assertTrue(ForgeUrlValidator.isAllowed("http://[2606:2800:220:1:248:1893:25c8:1946]/", NONE));
    }

    // --- refused: the address ranges a forge fetch has no business reaching ---------------------

    @Test
    public void refusesLoopbackIpv4() {
        assertFalse(ForgeUrlValidator.isAllowed("http://127.0.0.1/store", NONE));
    }

    @Test
    public void refusesLoopbackIpv6() {
        assertFalse(ForgeUrlValidator.isAllowed("http://[::1]/store", NONE));
    }

    @Test
    public void refusesTheLinkLocalMetadataAddress() {
        assertFalse(ForgeUrlValidator.isAllowed("http://169.254.169.254/latest/meta-data/", NONE));
    }

    @Test
    public void refusesPrivateTenDotRange() {
        assertFalse(ForgeUrlValidator.isAllowed("http://10.0.0.5/store", NONE));
    }

    @Test
    public void refusesPrivateOneNineTwoDotRange() {
        assertFalse(ForgeUrlValidator.isAllowed("http://192.168.1.10/store", NONE));
    }

    @Test
    public void refusesPrivateOneSevenTwoDotRange() {
        assertFalse(ForgeUrlValidator.isAllowed("http://172.16.5.4/store", NONE));
    }

    @Test
    public void refusesTheWildcardAddress() {
        assertFalse(ForgeUrlValidator.isAllowed("http://0.0.0.0/store", NONE));
    }

    @Test
    public void refusesAMulticastAddress() {
        assertFalse(ForgeUrlValidator.isAllowed("http://224.0.0.1/store", NONE));
    }

    // --- refused: not an absolute http(s) URL ---------------------------------------------------

    @Test
    public void refusesTheFileScheme() {
        assertFalse(ForgeUrlValidator.isAllowed("file:///etc/passwd", NONE));
    }

    @Test
    public void refusesTheFtpScheme() {
        assertFalse(ForgeUrlValidator.isAllowed("ftp://93.184.216.34/store", NONE));
    }

    @Test
    public void refusesAnUnknownScheme() {
        assertFalse(ForgeUrlValidator.isAllowed("gopher://93.184.216.34/", NONE));
    }

    @Test
    public void refusesASchemeRelativeUrlWithNoScheme() {
        assertFalse(ForgeUrlValidator.isAllowed("//93.184.216.34/store", NONE));
    }

    @Test
    public void refusesAUrlWithNoHost() {
        assertFalse(ForgeUrlValidator.isAllowed("http:///store", NONE));
    }

    @Test
    public void refusesMalformedInput() {
        assertFalse(ForgeUrlValidator.isAllowed("not a url", NONE));
    }

    @Test
    public void refusesBlankInput() {
        assertFalse(ForgeUrlValidator.isAllowed("   ", NONE));
        assertFalse(ForgeUrlValidator.isAllowed("", NONE));
        assertFalse(ForgeUrlValidator.isAllowed(null, NONE));
    }

    // --- the operator allow-list opts a genuinely internal forge back in ------------------------

    @Test
    public void acceptsAnInternalHostThatTheOperatorAllowListed() {
        Set<String> allowed = new HashSet<>();
        allowed.add("127.0.0.1");
        assertTrue(ForgeUrlValidator.isAllowed("http://127.0.0.1/store", allowed));
    }

    @Test
    public void theAllowListIsMatchedCaseInsensitivelyOnTheHost() {
        Set<String> allowed = new HashSet<>();
        allowed.add("internal-forge.local");
        // the host does not resolve, but the allow-list short-circuits before resolution
        assertTrue(ForgeUrlValidator.isAllowed("http://INTERNAL-FORGE.local/store", allowed));
    }

    @Test
    public void anAllowListForAnotherHostDoesNotRescueALoopbackUrl() {
        Set<String> allowed = new HashSet<>();
        allowed.add("some-other-host.local");
        assertFalse(ForgeUrlValidator.isAllowed("http://127.0.0.1/store", allowed));
    }
}

import java.nio.file.FileSystems
import java.nio.file.Files

def tempFileMarker = FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir"),"forge_nodes_migrated.txt")
if(!Files.exists(tempFileMarker)){
    throw new Exception("Temporary file forge_nodes_migrated.txt doesn't exist")
}
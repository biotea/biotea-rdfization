package ws.biotea.ld2rdf.rdfGeneration.orcid;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;

public class OrcidTest {

	@Test
	public void test() throws MalformedURLException, IOException {
		OrcidService service = OrcidServiceImpl.getInstance();
		String res = service.getOrcid("10.1371/journal.ppat.1002473", "Damià", "Garriga");
		assertNotNull(res);
	}

}

import org.junit.*;
import java.util.*;
import play.test.*;
import models.*;

public class ModelTest extends UnitTest {
    @Before
    public void setUp () {
        // clear the db
        Fixtures.deleteAll();
        Fixtures.loadModels("relationships.yml");
    }

    @Test
    public void testInstatiation () {
        
    }

    @Test
    public void testMetroAreaToString () {
        // TODO: assuming the autogenerated IDs are sequential from 0 isn't safe. Something
        // must be done.
        MetroArea sf = MetroArea.find("byName", "San Francisco-Oakland-San José, CA").first();
        MetroArea empty = MetroArea.find("byId", 1L).first();
        MetroArea full = ((NtdAgency) NtdAgency.find("byWebsite", "http://metro.kingcounty.gov")
                          .first())
            .metroArea;
        MetroArea fullNoAgencyName = ((NtdAgency) NtdAgency.find("byWebsite", "http://example.com")
                                      .first())
            .metroArea;
        assertEquals("San Francisco-Oakland-San José, CA", sf.toString());
        //        assertEquals("Empty metro area", empty.toString());
        assertEquals("Metro including King County Metro", full.toString());
        assertEquals("Metro including http://example.com", fullNoAgencyName.toString());
    }

    @Test
    public void testAgencyFeedRelation () {
        NtdAgency bart = NtdAgency.find("byWebsite", "http://www.bart.gov").first();
        NtdAgency kcm = NtdAgency.find("byWebsite", "http://metro.kingcounty.gov").first();

        assertNotNull(bart);
        assertNotNull(kcm);

        assertEquals(2, bart.feeds.size());
        assertEquals(2, kcm.feeds.size());
        
        // test that the feeds are allocated correctly and that the shared feed is shared
        GtfsFeed[] bartGtfs = new GtfsFeed[2];
        GtfsFeed[] kcmGtfs = new GtfsFeed[2];
        bart.feeds.toArray(bartGtfs);
        kcm.feeds.toArray(kcmGtfs);

        // this is the combined GTFS
        assertEquals(bartGtfs[1], kcmGtfs[1]);
        assertEquals("http://www.bart.gov", bartGtfs[0].agencyWebsite);
        assertEquals("http://metro.kingcounty.gov", kcmGtfs[0].agencyWebsite);
    }

    @Test
    public void testMetroAreaHasAgencies () {
        MetroArea sf = MetroArea.find("byName", "San Francisco-Oakland-San José, CA").first();
        
        assertNotNull(sf);
        assertEquals(1, sf.getAgencies().size());

        assertEquals("BART", sf.getAgencies().get(0).name);
    }

    @Test
    public void testFeedHasAgencies () {
        GtfsFeed mergedFeed = GtfsFeed.find("byAgencyWebsite", "http://example.com").first();
        
        assertNotNull(mergedFeed);
        
        assertEquals(2, mergedFeed.getAgencies().size());
    }

    @Test
    public void testBidirectionalRelationships () {
        // TODO: create constructors, maybe project lombok
        NtdAgency agency = new NtdAgency();
        GtfsFeed feed = new GtfsFeed();
        MetroArea metro = new MetroArea();

        agency.name = "The Funicular";
        agency.website = "http://funicular.example.com";
        agency.ntdId = "99991";
        agency.population = 2000;

        feed.country = "Mars";
        feed.feedBaseUrl = "http://funicular.example.com/gtfs";
        feed.official = true;
        feed.agencyWebsite = "http://funicular.example.com";

        metro.name = "Los Angeles, CA";

        agency.metroArea = metro;
        assertNotNull(agency.feeds);
        agency.feeds.add(feed);

        // wrt http://stackoverflow.com/questions/8169279
        metro.save();
        agency.save();
        feed.save();

        // confirm that they worked in the direction they were defined
        assertEquals(metro, agency.metroArea);
        // Set does not provide a get method.
        assertEquals(feed, agency.feeds.toArray()[0]);

        // confirm that the bidirectionality worked correctly
        assertEquals(1, metro.getAgencies().size());
        assertEquals(agency, metro.getAgencies().get(0));

        assertEquals(1, feed.getAgencies().size());
        assertEquals(agency, feed.getAgencies().get(0));
    }

    @Test
    public void testArgumentedConstructors () {
        NtdAgency agency = new NtdAgency("The Funicular", "http://example.net", "09999", 100000,
                                         590000, 4000000);

        agency.save();
        
        assertNotNull(agency.feeds);
        assertEquals(0, agency.feeds.size());
        assertEquals("The Funicular", agency.name);
        assertEquals("09999", agency.ntdId);
        assertEquals(100000, agency.population);
        assertEquals(590000, agency.ridership);
        assertEquals(4000000, agency.passengerMiles);
    }
}
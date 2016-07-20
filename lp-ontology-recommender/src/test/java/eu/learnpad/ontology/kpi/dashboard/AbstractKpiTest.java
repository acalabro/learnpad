/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.learnpad.ontology.kpi.dashboard;

import com.hp.hpl.jena.ontology.OntModel;
import eu.learnpad.ontology.AbstractUnitTest;
import eu.learnpad.ontology.notification.NotificationLog;
import eu.learnpad.ontology.persistence.FileOntAO;
import eu.learnpad.ontology.recommender.Inferencer;
import eu.learnpad.ontology.recommender.RecommenderException;
import eu.learnpad.ontology.transformation.SimpleModelTransformator;
import eu.learnpad.or.rest.data.NotificationActionType;
import eu.learnpad.or.rest.data.ResourceType;
import org.junit.Before;

/**
 *
 * @author sandro.emmenegger
 */
public class AbstractKpiTest extends AbstractUnitTest {
    
    public AbstractKpiTest() {
    }

    @Before
    public void setupTestData() throws RecommenderException {
        new Inferencer(getLatestModel());
        String pageUrl = "http://learnpad.eu/unittest/TestPage";
        Long timestamp = System.currentTimeMillis();
        NotificationLog.getInstance().logResourceNotification(MODELSET_ID, pageUrl, ResourceType.PAGE, null, null, TEST_USER, timestamp, NotificationActionType.ADDED);
        NotificationLog.getInstance().logResourceNotification(MODELSET_ID, "1", ResourceType.COMMENT, pageUrl, null, TEST_USER, timestamp, NotificationActionType.ADDED);
    }

    protected OntModel getLatestModel() throws RecommenderException {
        String latestModelSetVersion = SimpleModelTransformator.getInstance().getLatestModelSetId();
        OntModel model = FileOntAO.getInstance().getModelWithExecutionData(latestModelSetVersion);
        return model;
    }
    
}
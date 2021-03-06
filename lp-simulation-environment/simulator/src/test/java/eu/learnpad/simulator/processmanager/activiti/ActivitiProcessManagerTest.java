/**
 *
 */
package eu.learnpad.simulator.processmanager.activiti;

/*
 * #%L
 * LearnPAd Simulator
 * %%
 * Copyright (C) 2014 - 2015 Linagora
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import eu.learnpad.simulator.IProcessEventReceiver;
import eu.learnpad.simulator.Main;
import eu.learnpad.simulator.monitoring.event.impl.TaskStartSimEvent;
import eu.learnpad.simulator.utils.BPMNExplorer;
import eu.learnpad.simulator.utils.BPMNExplorerRepository;

/**
 * @author Tom Jorquera - Linagora
 *
 */
public class ActivitiProcessManagerTest {
	static ProcessEngine processEngine;

	public final static String TEST_PROCESS = "test.bpmn20.xml";

	public final static List<String> TEST_PROCESS_USES = Arrays.asList("user1",
			"user2", "user3");

	@Before
	public void initActivitiEngine() {
		ProcessEngineConfiguration config = ProcessEngineConfiguration
				.createProcessEngineConfigurationFromInputStream(Main.class
						.getClassLoader().getResourceAsStream(
								Main.ACTIVITY_CONFIG_PATH));

		// create process engine
		processEngine = config.buildProcessEngine();

	}

	@After
	public void shutdownEngine() {
		processEngine.close();
	}

	@Test
	public void testProcessDefinition() throws FileNotFoundException {

		ActivitiProcessManager manager = new ActivitiProcessManager(
				processEngine,
				mock(IProcessEventReceiver.IProcessEventReceiverProvider.class),
				mock(BPMNExplorerRepository.class));

		assertTrue(manager.getAvailableProcessDefintion().size() == 0);

		manager.addProjectDefinitions(TEST_PROCESS);

		assertTrue(manager.getAvailableProcessDefintion().size() == 1);

		String processDefinitionId = manager.getAvailableProcessDefintion()
				.iterator().next();

		assertTrue(manager.getProcessDefinitionName(processDefinitionId)
				.equals("Test process"));
	}

	@Test
	public void testProcessDefinitionRoles() throws FileNotFoundException {
		ActivitiProcessManager manager = new ActivitiProcessManager(
				processEngine,
				mock(IProcessEventReceiver.IProcessEventReceiverProvider.class),
				mock(BPMNExplorerRepository.class));

		String processDefinitionId = manager
				.addProjectDefinitions(TEST_PROCESS).iterator().next();

		assertTrue(manager.getProcessDefinitionGroupRoles(processDefinitionId)
				.containsAll(Arrays.asList("user1", "user2", "user3"))
				&& manager.getProcessDefinitionGroupRoles(processDefinitionId)
				.size() == 3);

		assertTrue(manager.getProcessDefinitionSingleRoles(processDefinitionId)
				.contains("user0"));
	}

	@SuppressWarnings("serial")
	@Test
	public void testProcessInstantation() throws FileNotFoundException {

		final IProcessEventReceiver processEventReceiver = mock(IProcessEventReceiver.class);

		IProcessEventReceiver.IProcessEventReceiverProvider provider = new IProcessEventReceiver.IProcessEventReceiverProvider() {

			public IProcessEventReceiver processEventReceiver() {
				return processEventReceiver;
			}
		};

		final BPMNExplorerRepository bpmnRepo = mock(BPMNExplorerRepository.class);

		doAnswer(new Answer<BPMNExplorer>() {
			public BPMNExplorer answer(InvocationOnMock invocation) {
				return mock(BPMNExplorer.class);
			}
		}).when(bpmnRepo).getExplorer(any(String.class));

		ActivitiProcessManager manager = new ActivitiProcessManager(
				processEngine, provider, bpmnRepo);

		String processDefinitionId = manager
				.addProjectDefinitions(TEST_PROCESS).iterator().next();

		assertTrue(manager.getCurrentProcessInstances().size() == 0);

		String processDefKey = manager
				.getProcessDefinitionKey(processDefinitionId);

		String processInstanceId = manager.startProjectInstance(processDefKey,
				new HashMap<String, Object>(),
				Arrays.asList("user1", "user2", "user3"),
				new HashMap<String, Collection<String>>() {
					{
						put("user1", Arrays.asList("user1"));
						put("user2", Arrays.asList("user2"));
						put("user3", Arrays.asList("user3"));

					}
				});

		// if we stop right now, this will cause an exception during
		// shutdownEngine (the test still pass). This is because the code to
		// submit tasks is done in a different thread, and may try to access the
		// activiti database *after* it has shutdown. The verify allows us to
		// "block" until the task submission thread has done its job.
		// 5 seconds should be *far more* than enough for this.
		ArgumentCaptor<TaskStartSimEvent> taskEvent = ArgumentCaptor
				.forClass(TaskStartSimEvent.class);

		verify(processEventReceiver, timeout(5000)).receiveTaskStartEvent(
				taskEvent.capture());
		assertEquals(taskEvent.getValue().task.processId, processInstanceId);

	}
}

/*******************************************************************************
 * Copyright (c) 2020 THALES GLOBAL SERVICES.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *    Thales - initial API and implementation
 *******************************************************************************/
package org.polarsys.capella.scenario.editor.embeddededitor.commands;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.sirius.diagram.DDiagram;
import org.eclipse.sirius.diagram.sequence.SequenceDDiagram;
import org.eclipse.sirius.diagram.sequence.business.internal.layout.flag.SequenceEventAbsoluteBoundsFlagger;
import org.eclipse.sirius.diagram.sequence.business.internal.operation.SynchronizeGraphicalOrderingOperation;
import org.eclipse.sirius.diagram.ui.business.internal.operation.AbstractModelChangeOperation;
import org.eclipse.sirius.viewpoint.description.AnnotationEntry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.xtext.resource.XtextResource;
import org.polarsys.capella.common.menu.dynamic.CreationHelper;
import org.polarsys.capella.core.data.capellacommon.AbstractState;
import org.polarsys.capella.core.data.capellacore.CapellacoreFactory;
import org.polarsys.capella.core.data.capellacore.Constraint;
import org.polarsys.capella.core.data.cs.ExchangeItemAllocation;
import org.polarsys.capella.core.data.fa.AbstractFunction;
import org.polarsys.capella.core.data.helpers.interaction.services.ExecutionEndExt;
import org.polarsys.capella.core.data.helpers.interaction.services.SequenceMessageExt;
import org.polarsys.capella.core.data.information.AbstractEventOperation;
import org.polarsys.capella.core.data.information.AbstractInstance;
import org.polarsys.capella.core.data.information.datavalue.DatavalueFactory;
import org.polarsys.capella.core.data.information.datavalue.OpaqueExpression;
import org.polarsys.capella.core.data.interaction.CombinedFragment;
import org.polarsys.capella.core.data.interaction.Event;
import org.polarsys.capella.core.data.interaction.EventReceiptOperation;
import org.polarsys.capella.core.data.interaction.EventSentOperation;
import org.polarsys.capella.core.data.interaction.Execution;
import org.polarsys.capella.core.data.interaction.ExecutionEnd;
import org.polarsys.capella.core.data.interaction.ExecutionEvent;
import org.polarsys.capella.core.data.interaction.FragmentEnd;
import org.polarsys.capella.core.data.interaction.InstanceRole;
import org.polarsys.capella.core.data.interaction.InteractionFactory;
import org.polarsys.capella.core.data.interaction.InteractionFragment;
import org.polarsys.capella.core.data.interaction.InteractionOperand;
import org.polarsys.capella.core.data.interaction.InteractionOperatorKind;
import org.polarsys.capella.core.data.interaction.InteractionState;
import org.polarsys.capella.core.data.interaction.MessageEnd;
import org.polarsys.capella.core.data.interaction.MessageKind;
import org.polarsys.capella.core.data.interaction.Scenario;
import org.polarsys.capella.core.data.interaction.SequenceMessage;
import org.polarsys.capella.core.data.interaction.StateFragment;
import org.polarsys.capella.core.data.interaction.TimeLapse;
import org.polarsys.capella.core.model.helpers.AbstractFragmentExt;
import org.polarsys.capella.core.model.helpers.ConstraintExt;
import org.polarsys.capella.core.model.helpers.ScenarioExt;
import org.polarsys.capella.scenario.editor.EmbeddedEditorInstance;
import org.polarsys.capella.scenario.editor.dsl.textualScenario.ArmTimerMessage;
import org.polarsys.capella.scenario.editor.dsl.textualScenario.Block;
import org.polarsys.capella.scenario.editor.dsl.textualScenario.CreateMessage;
import org.polarsys.capella.scenario.editor.dsl.textualScenario.DeleteMessage;
import org.polarsys.capella.scenario.editor.dsl.textualScenario.Message;
import org.polarsys.capella.scenario.editor.dsl.textualScenario.Operand;
import org.polarsys.capella.scenario.editor.dsl.textualScenario.Model;
import org.polarsys.capella.scenario.editor.dsl.textualScenario.Participant;
import org.polarsys.capella.scenario.editor.dsl.textualScenario.ParticipantDeactivation;
import org.polarsys.capella.scenario.editor.dsl.textualScenario.SequenceMessageType;
import org.polarsys.capella.scenario.editor.dsl.provider.TextualScenarioProvider;
import org.polarsys.capella.scenario.editor.embeddededitor.helper.XtextEditorHelper;
import org.polarsys.capella.scenario.editor.embeddededitor.views.EmbeddedEditorView;
import org.polarsys.capella.scenario.editor.helper.DslConstants;
import org.polarsys.capella.scenario.editor.helper.EmbeddedEditorInstanceHelper;

public class XtextToDiagramCommands {
  public static void process(Scenario scenario, EmbeddedEditorView embeddedEditorViewPart) {
    if (embeddedEditorViewPart != null) {
      TextualScenarioProvider p = embeddedEditorViewPart.getProvider();
      XtextResource resource = p.getResource();

      if (HelperCommands.isValidTextResource(resource)) {
        EList<EObject> content = resource.getContents();
        Model domainModel = (Model) content.get(0);

        // get participants
        EList<Participant> participants = domainModel.getParticipants();

        // get messages
        EList<EObject> messages = domainModel.getElements();

        doEditingOnParticipants(scenario, participants);

        doEditingOnElements(scenario, messages);

        // do refresh - when the messages associated with the removed actors are deleted too,
        // a refresh is needed to update also the editor
        EmbeddedEditorView eeView = XtextEditorHelper.getActiveEmbeddedEditorView();
        Scenario scenarioDiagram = EmbeddedEditorInstance.getAssociatedScenarioDiagram();
        DiagramToXtextCommands.process(scenarioDiagram, eeView);
      } else {
        MessageDialog.openError(Display.getCurrent().getActiveShell(), "Invalid data",
            "Please fix the errors in the textual editor!");
      }
    }
  }

  /**
   * Generates the participants in the Capella diagram
   * 
   * @param scenario
   *          The scenario diagram
   * @param participants
   *          List of participants defined in the xtext editor
   * @param messages
   *          
   */
  private static void doEditingOnParticipants(Scenario scenario, EList<Participant> participants) {
    // Make sure your element is attached to a resource, otherwise this will return null
    TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(scenario);
    domain.getCommandStack().execute(new RecordingCommand(domain) {

      @Override
      protected void doExecute() {
        InstanceRole instanceRole;
        EList<InstanceRole> instanceRoles = scenario.getOwnedInstanceRoles();

        for (Iterator<Participant> iterator = participants.iterator(); iterator.hasNext();) {
          EObject participant = iterator.next();

          String instanceName = ((Participant) participant).getName();
          // if the participant doesn't exist, create it
          if (instanceRoles.stream().filter(ir -> ir.getName().equals(instanceName)).collect(Collectors.toList()).isEmpty()) {
            instanceRole = InteractionFactory.eINSTANCE.createInstanceRole();
            instanceRole.setName(instanceName);

            String keyword = ((Participant) participant).getKeyword();

            EObject capellaParticipant = null;

            List<? extends EObject> capellaParticipants = EmbeddedEditorInstanceHelper.getAvailableElements(keyword)
                .stream().filter(f -> ((AbstractInstance) f).getName().equals(instanceName))
                .collect(Collectors.toList());
            if (!capellaParticipants.isEmpty()) {
              capellaParticipant = capellaParticipants.get(0);
            }

            instanceRole.setRepresentedInstance((AbstractInstance) capellaParticipant);
            instanceRoles.add(instanceRole);
          }
        }

        // remove all instance roles from diagram that do not exist in editor
        removeParticipantsFromDiagram(scenario, participants);

        // do instance roles reorder if they do not match the order of the editor participants
        reorderParticipants(instanceRoles, participants);
      }
    });
  }

  /**
   * Remove all instance roles that are in the diagram, but not in the editor. 
   * Remove all related elements.
   * 
   * @param scenario
   *          The scenario diagram
   * @param participants
   *          Participants from editor
   */
  private static void removeParticipantsFromDiagram(Scenario scenario,
      EList<Participant> participants) {
    List<InstanceRole> irToRemove = new ArrayList<>();
    EList<InstanceRole> instanceRoles = scenario.getOwnedInstanceRoles();
    
    for (InstanceRole ir : instanceRoles) {
      List<String> participantsName = participants.stream().map(x -> x.getName()).collect(Collectors.toList());
      if (!participantsName.contains(ir.getName())) {
        irToRemove.add(ir);
        // remove all related diagram elements
        removeParticipantRelatedElements(scenario, ir);
      }
    }
    for (InstanceRole ir : irToRemove) {
      instanceRoles.remove(ir);
    }
  }

  /**
   * Remove all related elements to the giver instance role.
   * 
   * @param scenario
   *          The scenario diagram
   * @param instanceRole
   *          Instance role for which we want to remove the related elements
   */
  private static void removeParticipantRelatedElements(Scenario scenario, InstanceRole instanceRole) {
    removeParticipantRelatedMessages(scenario, instanceRole.getName());
    removeParticipantRelatedStateFragments(scenario, instanceRole.getName());
    removeParticipantRelatedCombinedFragments(scenario, instanceRole);
  }

  /**
   * Order participants in Capella diagram in the same order they appear in editor.
   * 
   * @param instanceRoles
   *          List of instance roles in the diagram
   * @param participants
   *          List of participants in the editor
   * @return true if the order was changed, false if not
   */
  private static boolean reorderParticipants(List<InstanceRole> instanceRoles, EList<Participant> participants) {
    boolean updateNeeded = false;
    for (int i = 0; i < instanceRoles.size(); i++) {
      if (!instanceRoles.get(i).getName().equals(participants.get(i).getName())) {
        for (int j = 0; j < participants.size(); j++) {
          if (instanceRoles.get(i).getName().equals(participants.get(j).getName())) {
            ((EList<InstanceRole>) instanceRoles).move(j, instanceRoles.get(i));
            updateNeeded = true;
          }
        }
      }
    }
    return updateNeeded;
  }

  /**
   * Remove all messages containing the participant that has just been deleted
   * 
   * @param scenario
   *          The scenario diagram
   * @param participantName
   *          The participant that has just been deleted
   */
  private static void removeParticipantRelatedMessages(Scenario scenario, String participantName) {
    List<SequenceMessage> messagesToBeDeleted = new ArrayList();    
    EList<SequenceMessage> messages = scenario.getOwnedMessages();   
    
    for (SequenceMessage message : messages) {
      if (message.getSendingEnd().getCoveredInstanceRoles().get(0).getName().equals(participantName) ||
          message.getReceivingEnd().getCoveredInstanceRoles().get(0).getName().equals(participantName)) {
          messagesToBeDeleted.add(message);
       }
    }
    for (SequenceMessage message : messagesToBeDeleted) {
      removeMessageFromScenario(scenario, message);
    }
  }

  /**
   * Remove from diagram all messages related to the participant that has just been deleted
   * 
   * @param scenario
   *          The scenario diagram
   * @param participantName
   *          The participant that has just been deleted
   */
  protected static void removeParticipantRelatedStateFragments(Scenario scenario, String participantName) {
    List<TimeLapse> stateFragmentsToBeDeleted = scenario.getOwnedTimeLapses().stream()
        .filter(timelapse -> timelapse instanceof StateFragment 
            && timelapse.getStart().getCoveredInstanceRoles().get(0).getName().equals(participantName))
        .collect(Collectors.toList());
    
    for (TimeLapse timeLapse : stateFragmentsToBeDeleted) {
      removeStateFragmentFromScenario(scenario, timeLapse);
    } 
  }

  /**
   * Remove from diagram all combined fragment related to the participant that has just been deleted
   * 
   * @param scenario
   *          The scenario diagram
   * @param participantName
   *          The participant that has just been deleted
   */
  private static void removeParticipantRelatedCombinedFragments(Scenario scenario, InstanceRole instanceRole) {
    List<TimeLapse> combinedFragmentsToBeDeleted = scenario.getOwnedTimeLapses().stream()
        .filter(timelapse -> timelapse instanceof CombinedFragment
            && timelapse.getStart().getCoveredInstanceRoles().contains(instanceRole))
        .collect(Collectors.toList());
    
    for (TimeLapse timeLapse : combinedFragmentsToBeDeleted) {
      removeCombinedFragmentFromScenario(scenario, timeLapse);
    }   
  }
  
  /**
   * Synchronize graphical ordering in diagram
   */
  private static void syncGraphicalOrdering() {
    DDiagram dDiagram = EmbeddedEditorInstance.getDDiagram();
    ((SequenceDDiagram) dDiagram).getTarget();
    EList<AnnotationEntry> ownedAnnotationEntries = dDiagram.getOwnedAnnotationEntries();
    EObject data = null;
    for (AnnotationEntry annotationEntry : ownedAnnotationEntries) {
      if ((annotationEntry != null) && (annotationEntry.getData() instanceof Diagram)) {
        data = annotationEntry.getData();
      }
    }
    AbstractModelChangeOperation<Boolean> synchronizeGraphicalOrderingOperation = new SynchronizeGraphicalOrderingOperation(
        (Diagram) data, true);
    synchronizeGraphicalOrderingOperation.execute();
  }

  /**
   * Create and put in order all objects necessary to display the text scenario in the Capella diagram. 
   * This includes sequence messages, combined fragments, state fragments
   * 
   * @param scenario
   *          The scenario diagram
   * @param elements
   *          The list of elements in editor
   */
  private static void doEditingOnElements(Scenario scenario, EList<EObject> elements) {
    // Make sure your element is attached to a resource, otherwise this will return null
    TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(scenario);
    domain.getCommandStack().execute(new RecordingCommand(domain) {

      @Override
      protected void doExecute() {
        // remove messages and state fragments that appear in diagram and do not appear in text
        cleanUpMessages(scenario, elements);
        cleanUpStateFragments(scenario, elements);
        cleanUpCombinedFragments(scenario, elements);

        editElements(scenario, elements);

        // Reorder scenario, this means reordering the interaction fragments and sequence messages lists
        reorderCapellaScenario(scenario, elements);
        // refresh visual editor
        syncGraphicalOrdering();
      }

      /**
       * Reorder interaction fragments and messages (their start, end and execution end) according to the xtext
       * scenario
       * 
       * @param scenario
       *          The scenario diagram
       * @param elements
       *          The list of elements in editor
       */
      private void reorderCapellaScenario(Scenario scenario, EList<EObject> elements) {
        // compose new lists of sequence messages and interaction fragments, ordered based on xtext messages
        List<SequenceMessage> capellaSequenceMessages = new ArrayList<>();
        List<InteractionFragment> interactionFragments = new ArrayList<>();

        List<InteractionFragment> executionEndsToProcess = new ArrayList<>();
        reorderCapellaFragments(scenario, elements, capellaSequenceMessages, interactionFragments,
            executionEndsToProcess);

        // Replace sequence message list and interaction fragments list in the real scenario 
        // with the newly computed lists
        scenario.getOwnedInteractionFragments().clear();
        scenario.getOwnedInteractionFragments().addAll(interactionFragments);

        scenario.getOwnedMessages().clear();
        scenario.getOwnedMessages().addAll(capellaSequenceMessages);
      }

      /**
       * Reorder interaction fragments and messages (their start, end and execution end) according to the xtext
       * scenario
       * 
       * @param scenario
       *          The scenario diagram
       * @param elements
       *          The list of elements in editor
       */
      private void reorderCapellaFragments(Scenario scenario, EList<EObject> elements,
          List<SequenceMessage> capellaSequenceMessages, List<InteractionFragment> interactionFragments,
          List<InteractionFragment> executionEndsToProcess) {

        for (Iterator<EObject> iterator = elements.iterator(); iterator.hasNext();) {
          EObject elementFromXtext = iterator.next();

          if (elementFromXtext instanceof ParticipantDeactivation) {
            reorderCapellaDeactivationMessages((ParticipantDeactivation) elementFromXtext,
                interactionFragments, capellaSequenceMessages, executionEndsToProcess);
          } else if (elementFromXtext instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.Message) {
            reorderCapellaSequenceMessages(scenario,
                (org.polarsys.capella.scenario.editor.dsl.textualScenario.Message) elementFromXtext,
                interactionFragments, capellaSequenceMessages, executionEndsToProcess);
          } else if (elementFromXtext instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment) {
            org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment textCombinedFragment = (org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment) elementFromXtext;
            CombinedFragment capellaCombinedFragment = getCorrespondingCapellaCombinedFragment(scenario,
                textCombinedFragment);
            if (capellaCombinedFragment != null) {
              // start the Combined Fragment Block
              interactionFragments.add(capellaCombinedFragment.getStart());

              List<InteractionOperand> orderedCapellaOperands = 
                  getOrderedCapellaInteractionOperands(scenario, textCombinedFragment, capellaCombinedFragment);
              
              // add content in the first operand of the Combined Fragment Block
              interactionFragments.add(orderedCapellaOperands.get(0));
              reorderCapellaFragments(scenario, textCombinedFragment.getBlock().getBlockElements(),
                  capellaSequenceMessages, interactionFragments, executionEndsToProcess);

              // reorder the other operands
              EList<Operand> textOperands = textCombinedFragment.getOperands();
              for (int i = 0; i < textOperands.size(); i++) {
                InteractionOperand operand = orderedCapellaOperands.get(i + 1);
                // add content in the first operand of the Combined Fragment Block
                interactionFragments.add(operand);
                reorderCapellaFragments(scenario, textOperands.get(i).getBlock().getBlockElements(),
                    capellaSequenceMessages, interactionFragments, executionEndsToProcess);
              }

              // finish the Combined Fragment Block
              interactionFragments.add(capellaCombinedFragment.getFinish());
            }
          } else if (elementFromXtext instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment) {

            StateFragment stateFragment = getCorrespondingCapellaStateFragment(scenario, interactionFragments,
                (org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment) elementFromXtext);
            if (stateFragment != null) {
              interactionFragments.add(stateFragment.getStart());
              interactionFragments.add(stateFragment.getFinish());
            }
          }
        }
      }

      /**
       * Move the interaction fragment representing the execution end to the correct position in the interaction
       * fragments list, according to the xtext scenario
       * 
       * @param scenario
       *          The scenario diagram
       * @param participantDeactivationMessage
       *          The ParticipantDeactivation message in the xtext scenario
       * @param interactionFragments
       *          The new list of ordered interaction fragments that will be used to update the Capella diagram
       *          according to the xtext scenario
       * @param capellaSequenceMessages
       *          The new list of Capella sequence messages that will be used to update the Capella diagram
       *          according to the xtext scenario
       * @param executionEndsToProcess
       *          List of execution ends that have to be processed (moved on the correct order in the list of
       *          interaction fragments.
       */
      private void reorderCapellaDeactivationMessages(ParticipantDeactivation participantDeactivationMessage,
          List<InteractionFragment> interactionFragments,
          List<SequenceMessage> capellaSequenceMessages,
          List<InteractionFragment> executionEndsToProcess) {
        // This is a ParticipantDeactivationMessage, so the execution on the corresponding timeline finished.
        // We must move the interaction fragment representing the execution end on the correct position in the
        // ownedInteractionFragments ordered list

        // find the timeline (instance role) of the execution that has to end. Search by participant name
        InstanceRole instanceRole = EmbeddedEditorInstanceHelper
            .getInstanceRole(participantDeactivationMessage.getName());

        doDeactivationSequenceMessageForReorder(interactionFragments, instanceRole, executionEndsToProcess,
            capellaSequenceMessages);
      }

      /**
       * Move the interaction fragments belonging to a sequence message to the correct position in the interaction
       * fragments list, according to the xtext scenario
       * 
       * @param scenario
       *          The scenario diagram
       * @param elementFromXtext
       *          The element in the xtext scenario
       * @param interactionFragments
       *          The new list of ordered interaction fragments that will be used to update the Capella diagram
       *          according to the xtext scenario
       * @param capellaSequenceMessages
       *          The new list of Capella sequence messages that will be used to update the Capella diagram
       *          according to the xtext scenario
       * @param executionEndsToProcess
       *          List of execution ends that have to be processed (moved on the correct order in the list of
       *          interaction fragments.
       */
      private void reorderCapellaSequenceMessages(Scenario scenario, Message elementFromXtext,
          List<InteractionFragment> interactionFragments, List<SequenceMessage> capellaSequenceMessages,
          List<InteractionFragment> executionEndsToProcess) {
        // This is a sequence message, it can be with execution and/or with return
        if (foundInstanceRolesOnMessageEnds(elementFromXtext)) {
          SequenceMessage capellaSequenceMessage = getCorrespondingCapellaSequenceMessage(scenario, elementFromXtext);
          if (capellaSequenceMessage != null) {
            capellaSequenceMessages.add(capellaSequenceMessage);

            // insert message ends in interaction fragments list
            interactionFragments.add(capellaSequenceMessage.getSendingEnd());
            interactionFragments.add(capellaSequenceMessage.getReceivingEnd());

            if (!(capellaSequenceMessage.getKind().equals(MessageKind.CREATE)
                || capellaSequenceMessage.getKind().equals(MessageKind.DELETE))) {

              Execution execution = getExecutionForSequenceMessage(scenario, capellaSequenceMessage);
              // For simple messages, execution end will be added here, if no return branch
              if (!hasExecution(elementFromXtext)) {
                if (!hasReturn(elementFromXtext)) {
                  if(execution != null)
                    interactionFragments.add(execution.getFinish());
                } else {
                  // Simple message has return branch, add the return sequence message here
                  SequenceMessage capellaReturnSequenceMessage = SequenceMessageExt
                      .getOppositeSequenceMessage(capellaSequenceMessage);
                  if (capellaReturnSequenceMessage == null) {
                    capellaReturnSequenceMessage = SequenceMessageExt.findReplySequenceMessage(capellaSequenceMessage);
                  }
                  interactionFragments.add(capellaReturnSequenceMessage.getSendingEnd());
                  interactionFragments.add(capellaReturnSequenceMessage.getReceivingEnd());
                  capellaSequenceMessages.add(capellaReturnSequenceMessage);
                }
              } else {
                // For withExecution messages, execution end will be added when deactivate message will be found.
                // At that point, the return message will be moved in order, too, if this is the case
                if(execution != null)
                  executionEndsToProcess.add(execution.getFinish());
              }
            }
          }
        }
      }

      /**
       * Check if message ends are on valid timelines (instance roles)
       * 
       * @param elementFromXtext
       *          The element in the xtext scenario
       * @return true if instance roles found, false otherwise
       */
      private boolean foundInstanceRolesOnMessageEnds(Message elementFromXtext) {
        if (elementFromXtext instanceof SequenceMessageType) {   
          InstanceRole source = EmbeddedEditorInstanceHelper.getInstanceRole(((SequenceMessageType) elementFromXtext).getSource());
          InstanceRole target = EmbeddedEditorInstanceHelper.getInstanceRole(((SequenceMessageType) elementFromXtext).getTarget());
          return source != null && target != null;
        }
        if (elementFromXtext instanceof ArmTimerMessage) {
          InstanceRole timeline = EmbeddedEditorInstanceHelper.getInstanceRole(((ArmTimerMessage) elementFromXtext).getParticipant());
          return timeline != null;
        }
        return false;
      }
      
    });
  }

  /**
   * Create all objects necessary to display the text scenario in the Capella diagram. 
   * This includes sequence messages, combined fragments, state fragments
   * 
   * @param scenario
   *          The scenario diagram
   * @param elements
   *          The list of elements in editor
   */
  private static void editElements(Scenario scenario, EList<EObject> elements) {
    EList<SequenceMessage> sequenceMessages = scenario.getOwnedMessages();
    List<EObject> previousStateFragments = new ArrayList <>();
    for (Iterator<EObject> iterator = elements.iterator(); iterator.hasNext();) {
      EObject xtextElement = iterator.next();

      if (xtextElement instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.Message
          && !(xtextElement instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.ParticipantDeactivation)) {
        // This is a sequence message, it can be with execution and/or with return
        org.polarsys.capella.scenario.editor.dsl.textualScenario.Message seqMessage = 
            (org.polarsys.capella.scenario.editor.dsl.textualScenario.Message) xtextElement;
        
        InstanceRole source = null;
        InstanceRole target = null;
        if (seqMessage instanceof SequenceMessageType) {
          source = EmbeddedEditorInstanceHelper.getInstanceRole(((SequenceMessageType) seqMessage).getSource());
          target = EmbeddedEditorInstanceHelper.getInstanceRole(((SequenceMessageType) seqMessage).getTarget());
        } else {
          source = EmbeddedEditorInstanceHelper.getInstanceRole(((ArmTimerMessage) seqMessage).getParticipant());
          target = source;
        }
        
        if (!foundMessageBySourceTargetAndName(sequenceMessages, source, target, seqMessage.getName())) {
          SequenceMessage sequenceMessage = createCapellaSequenceMessage(scenario, source, target, seqMessage);
          sequenceMessages.add(sequenceMessage);
          
          // if message has return branch, create corresponding Capella return message
          if (hasReturn(seqMessage)) {
            SequenceMessage opposingSequenceMessage = createCapellaSequenceMessage(scenario, target, source,
                seqMessage, true);
            Execution execution = getExecutionForSequenceMessage(scenario, sequenceMessage);
            if(execution != null)
              execution.setFinish(opposingSequenceMessage.getSendingEnd());
            sequenceMessages.add(opposingSequenceMessage);
          }
        }
      } else if (xtextElement instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment) {
        // Check if we need to create CombinedFragment

        CombinedFragment capellaFragment = getCorrespondingCapellaCombinedFragment(scenario,
            (org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment) xtextElement);
        if (capellaFragment == null) {
          InteractionFragment lastInteractionFragment = null;
          
          if (!sequenceMessages.isEmpty()) {
            SequenceMessage lastSequenceMessage = sequenceMessages.get(sequenceMessages.size() - 1);
            lastInteractionFragment = lastSequenceMessage.getReceivingEnd();
          }
          createCapellaCombinedFragmentBlock(scenario,
              (org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment) xtextElement,
              lastInteractionFragment);
        } else {
          // combined fragment found, check its contents
          editElements(scenario,
              ((org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment) xtextElement).getBlock()
                  .getBlockElements());
          for (Operand operand : ((org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment) xtextElement)
              .getOperands()) {
            editElements(scenario, operand.getBlock().getBlockElements());
          }
        }
      } else if (xtextElement instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment) {
        previousStateFragments.add(xtextElement);
        editStateFragment(scenario,
            (org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment) xtextElement,
            previousStateFragments);
      }
    }
  }

  /**
   * Check to see if there is any message with the given source, target and name
   * 
   * @param sequenceMessages
   *          List of sequence messages to search into
   * @param source
   *          Source instance role
   * @param target 
   *          Target instance role
   * @param messageName
   *          Message name
   * @return true if a message with these attributes was found, false otherwise
   */
  private static boolean foundMessageBySourceTargetAndName(EList<SequenceMessage> sequenceMessages, 
      InstanceRole source, InstanceRole target, String messageName) {
    if (source == null) {
      return false;
    }
    List<SequenceMessage> msgsFilteredByNameTargetSource = sequenceMessages.stream()
        .filter(x -> x.getName().equals(messageName))
        .filter(x -> x.getSendingEnd().getCoveredInstanceRoles().get(0).getName().equals(source.getName()))
        .filter(x -> x.getReceivingEnd().getCoveredInstanceRoles().get(0).getName().equals(target.getName()))
        .collect(Collectors.toList());
    return !msgsFilteredByNameTargetSource.isEmpty();
  }

  /**
   * Check to see if the given message has return branch
   * 
   * @param elementFromXtext
   *          The element representing the message in the xtext editor
   * @return true if message has return branch, false otherwise
   */
  private static boolean hasReturn(Message elementFromXtext) {
    if (elementFromXtext instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.SequenceMessage)
      return ((org.polarsys.capella.scenario.editor.dsl.textualScenario.SequenceMessage) elementFromXtext).getReturn() != null;
    return false;
  }
  
  /**
   * Check to see if the given message has execution (withExecution keyword present)
   * 
   * @param elementFromXtext
   *          The element representing the message in the xtext editor
   * @return true if message has execution, false otherwise
   */
  protected static boolean hasExecution(Message elementFromXtext) {
    if (elementFromXtext instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.SequenceMessage) {
      return ((org.polarsys.capella.scenario.editor.dsl.textualScenario.SequenceMessage) elementFromXtext).getExecution() != null;
    }
    if (elementFromXtext instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.ArmTimerMessage) {
      return ((org.polarsys.capella.scenario.editor.dsl.textualScenario.ArmTimerMessage) elementFromXtext).getExecution() != null;
    }
    return false;
  }

  /**
   * Create/update a state fragment using the information in the xtext scenario 
   * 
   * @param scenario
   *          The scenario diagram
   * @param xtextElement
   *          The element representing the state fragment in the xtext scenario
   * @param previousEditorStateFragments
   *           The list of previously occurring matching state fragments
   */
  private static void editStateFragment(Scenario scenario,
      org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment xtextElement,
      List<EObject> previousEditorStateFragments) {

    InstanceRole instanceRole = EmbeddedEditorInstanceHelper.getInstanceRole(xtextElement.getTimeline());
    EList<TimeLapse> ownedTimeLapses = scenario.getOwnedTimeLapses();

    List<EObject> simillarStateFragments = previousEditorStateFragments.stream()
        .filter(x -> ((org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment) x).getTimeline()
            .equals(xtextElement.getTimeline())
            && ((org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment) x).getName()
                .equals(xtextElement.getName())
            && ((org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment) x).getKeyword()
                .equals(xtextElement.getKeyword()))
        .collect(Collectors.toList());

    EObject relatedElement = getRelatedElement(instanceRole, xtextElement);

    if (relatedElement == null)
      return;

    List<TimeLapse> capellaStateFragments = getStateFragmentsWithGivenAttributes(scenario,
        instanceRole, xtextElement, relatedElement);

    if (capellaStateFragments.isEmpty() || capellaStateFragments.size() < simillarStateFragments.size()) {
      InteractionState interactionStateStart = createInteractionState(DslConstants.START, relatedElement, instanceRole);
      InteractionState interactionStateEnd = createInteractionState(DslConstants.FINISH, relatedElement, instanceRole);

      scenario.getOwnedInteractionFragments().add(interactionStateStart);
      scenario.getOwnedInteractionFragments().add(interactionStateEnd);

      StateFragment stateFragment = createStateFragment(interactionStateStart, interactionStateEnd, relatedElement);

      ownedTimeLapses.add(stateFragment);
    }
  }

  /**
   * Return a list of state fragments with the given attributes (instance role and related element) 
   * 
   * @param scenario
   *          The scenario diagram
   * @param instanceRole
   *          The timeline for this state fragment
   * @param xtextElement
   *          The element representing the state fragment in the xtext scenario
   * @param previousEditorStateFragments
   *           The list of previously occurring matching state fragments
   * @param relatedElement
   *          The element representing Capella abstract function or abstract state related with a state fragment
   * @return the list of state fragments with the given attributes
   */
  private static List<TimeLapse> getStateFragmentsWithGivenAttributes(Scenario scenario,
      InstanceRole instanceRole, org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment xtextElement,
      EObject relatedElement) {

    if (xtextElement.getKeyword().equals(DslConstants.FUNCTION))
      return scenario.getOwnedTimeLapses().stream()
          .filter(
              x -> x instanceof StateFragment && ((StateFragment) x).getRelatedAbstractFunction().equals(relatedElement)
                  && ((StateFragment) x).getStart().getCoveredInstanceRoles().get(0).equals(instanceRole))
          .collect(Collectors.toList());

    return scenario.getOwnedTimeLapses().stream()
        .filter(x -> x instanceof StateFragment && ((StateFragment) x).getRelatedAbstractState().equals(relatedElement)
            && ((StateFragment) x).getStart().getCoveredInstanceRoles().get(0).equals(instanceRole))
        .collect(Collectors.toList());
  }

  /**
   * Return the related abstract function or abstract state for a given state fragment
   * 
   * @param scenario
   *          The scenario diagram
   * @param instanceRole
   *          The timeline for this state fragment
   * @param stateFragment
   *          The element representing the state fragment in the xtext scenario
   * @return the related abstract function or abstract state for the given state fragment
   */
  private static EObject getRelatedElement(InstanceRole instanceRole,
      org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment stateFragment) {

    if (stateFragment.getKeyword().equals(DslConstants.FUNCTION)) {
      return getCorrespondingCapellaAbstractFunction(instanceRole, stateFragment.getName());
    }
    return getCorrespondingCapellaAbstractState(instanceRole, stateFragment.getName(),
        stateFragment.getKeyword());
  }

  /**
   * Create a state fragment with the given attributes
   * 
   * @param interactionStateStart
   *          InteractionState representing the start of the state fragment
   * @param interactionStateEnd
   *          InteractionState representing the end of the state fragment
   * @param relatedElement
   *          The related abstract function or abstract state for the given state fragment
   * @return the newly created state fragment
   */
  private static StateFragment createStateFragment(InteractionState interactionStateStart,
      InteractionState interactionStateEnd, EObject relatedElement) {

    StateFragment stateFragment = InteractionFactory.eINSTANCE.createStateFragment();
    stateFragment.setStart(interactionStateStart);
    stateFragment.setFinish(interactionStateEnd);

    if (relatedElement instanceof AbstractFunction) {
      stateFragment.setRelatedAbstractFunction((AbstractFunction) relatedElement);
    } else {
      stateFragment.setRelatedAbstractState((AbstractState) relatedElement);
    }

    return stateFragment;
  }

  /**
   * Create an interaction state with the given attributes
   * 
   * @param name
   *          The name of the interaction state
   * @param relatedElement
   *          The related abstract function or abstract state for the given state fragment
   * @param instanceRole
   *          Timeline (instance role) for the interaction state 
   * @return the newly created interaction state
   */
  private static InteractionState createInteractionState(String name, EObject relatedElement, InstanceRole instanceRole) {

    InteractionState interactionState = InteractionFactory.eINSTANCE.createInteractionState();
    interactionState.setName(name);
    interactionState.getCoveredInstanceRoles().add(instanceRole);

    if (relatedElement instanceof AbstractFunction) {
      interactionState.setRelatedAbstractFunction((AbstractFunction) relatedElement);
    } else {
      interactionState.setRelatedAbstractState((AbstractState) relatedElement);
    }

    return interactionState;
  }

  /**
   * Return the corresponding Capella abstract function with the given attributes
   * 
   * @param scenario
   *          The scenario diagram
   * @param instanceRole
   *          Timeline (instance role) for the interaction state 
   * @param name
   *          Name of the abstract function
   * @return the corresponding Capella abstract function or null if no abstract function with the given attributes was found
   */
  private static AbstractFunction getCorrespondingCapellaAbstractFunction(InstanceRole instanceRole,
      String name) {
    List<AbstractFunction> availableFunctions = EmbeddedEditorInstanceHelper.getAllocatedFunctions(instanceRole);

    for (AbstractFunction availableFunction : availableFunctions) {
      if (availableFunction.getName().equals(name))
        return availableFunction;
    }
    return null;
  }

  /**
   * Return the corresponding Capella abstract state with the given attributes
   * 
   * @param scenario
   *          The scenario diagram
   * @param instanceRole
   *          Timeline (instance role) for the interaction state 
   * @param name
   *          Name of the abstract state
   * @param keyword
   *          STATE or MODE
   * @return the corresponding Capella abstract state or null if no abstract state with the given attributes was found
   */
  private static AbstractState getCorrespondingCapellaAbstractState(InstanceRole instanceRole,
      String name, String keyword) {
    List<AbstractState> availableStates;

    if (keyword.equals(DslConstants.STATE)) {
      availableStates = EmbeddedEditorInstanceHelper.getStates(instanceRole);
    } else {
      availableStates = EmbeddedEditorInstanceHelper.getModes(instanceRole);
    }

    for (AbstractState availableState : availableStates) {
      if (availableState.getName().equals(name))
        return availableState;
    }
    return null;
  }

  /**
   * Return the Execution object for the given Capella sequence message
   * 
   * @param scenario
   *          The scenario diagram
   * @param sequenceMessage
   *          Capella sequence message 
   * @return the Execution object or null if no execution found
   */
  private static Execution getExecutionForSequenceMessage(Scenario scenario, SequenceMessage sequenceMessage) {
    Execution execution = null;
    MessageEnd receivingEnd = sequenceMessage.getReceivingEnd();
    for (TimeLapse timeLapse : scenario.getOwnedTimeLapses()) {
      if (timeLapse instanceof Execution) {
        Execution exec = (Execution) timeLapse;
        if (exec.getStart() != null && exec.getStart().equals(receivingEnd)) {
          execution = exec;
        }
      }
    }
    return execution;
  }

  /**
   * Return the corresponding Capella sequence message
   * 
   * @param scenario
   *          The scenario diagram
   * @param elementFromXtext
   *          The element representing the message in the xtext editor 
   * @return the corresponding Capella sequence message or null if no message found
   */
  private static SequenceMessage getCorrespondingCapellaSequenceMessage(Scenario scenario,
      Message elementFromXtext) {
    EList<SequenceMessage> sequenceMessages = scenario.getOwnedMessages();

    for (SequenceMessage sm : sequenceMessages) {
      if (isSameMessage(elementFromXtext, sm)) {
        return sm;
      }
    }
    return null;
  }

  /**
   * Return the corresponding Capella interaction operand inside a Capella combine fragment
   * 
   * @param scenario
   *          The scenario diagram
   * @param textOperandBlock
   *          The element representing the operand block in the xtext editor
   * @param capellaCombinedFragment
   *          Capella combined fragment where we search the interaction fragment
   * @param processedOperands
   *          List of already processed operands
   * @return the corresponding Capella interaction operand or null if no operand found
   */
  private static InteractionOperand getCapellaInteractionOperand(Scenario scenario,
      org.polarsys.capella.scenario.editor.dsl.textualScenario.Operand textOperandBlock,
      CombinedFragment capellaCombinedFragment, List<InteractionOperand> processedOperands) {
    List<InteractionOperand> combinedFragmentOperands = AbstractFragmentExt.getOwnedOperands(capellaCombinedFragment,
        scenario);
    combinedFragmentOperands.removeAll(processedOperands);
    for (InteractionOperand operand : combinedFragmentOperands) {
      // go trough each text operand and check that
      if (textOperandBlock.getExpression().equals(HelperCommands.getExpressionText(operand))) {
        processedOperands.add(operand);
        return operand;
      }
    }
    return null;
  }

  /**
   * Return the first Capella interaction operand inside a Capella combine fragment
   * 
   * @param scenario
   *          The scenario diagram
   * @param textCombinedFragment
   *          The element representing the combined fragment in the xtext editor
   * @param capellaCombinedFragment
   *          Capella combined fragment where we search the interaction fragment
   * @return the first corresponding Capella interaction operand or null if no operand found
   */
  private static InteractionOperand getFirstCapellaInteractionOperand(Scenario scenario,
      org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment textCombinedFragment,
      CombinedFragment capellaCombinedFragment) {
    List<InteractionOperand> combinedFragmentOperands = AbstractFragmentExt.getOwnedOperands(capellaCombinedFragment,
        scenario);

    for (InteractionOperand operand : combinedFragmentOperands) {
      // go trough each text operand and check that
      if (textCombinedFragment.getExpression().equals(HelperCommands.getExpressionText(operand))) {
        return operand;
      }
    }
    return null;
  }

  /**
   * Return the list of ordered Capella interaction operands inside a Capella combine fragment
   * 
   * @param scenario
   *          The scenario diagram
   * @param textCombinedFragment
   *          The element representing the combined fragment in the xtext editor
   * @param capellaCombinedFragment
   *          Capella combined fragment where we search the interaction fragment
   * @return the list of ordered Capella interaction operands
   */
  private static List<InteractionOperand> getOrderedCapellaInteractionOperands(Scenario scenario,
      org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment textCombinedFragment,
      CombinedFragment capellaCombinedFragment) {
    List<InteractionOperand> orderedOperands = new ArrayList<>(
        1 + textCombinedFragment.getOperands().size());

    orderedOperands.add(getFirstCapellaInteractionOperand(scenario, textCombinedFragment, capellaCombinedFragment));

    List<InteractionOperand> processedOperands = new ArrayList<>();
    processedOperands.add(orderedOperands.get(0));
    
    for (Operand textOperandBlock : textCombinedFragment.getOperands()) {
      orderedOperands.add(getCapellaInteractionOperand(scenario, textOperandBlock, capellaCombinedFragment, processedOperands));
    }

    return orderedOperands;
  }

  /**
   * Return the corresponding Capella state fragment for the given xtext element
   * 
   * @param scenario
   *          The scenario diagram
   * @param interactionFragments
   *          List of timelapses where we search for the state fragment
   * @param stateFragment
   *          The element representing the state fragment in the xtext editor
   * @return the corresponding Capella state fragment or null if not found
   */
  private static StateFragment getCorrespondingCapellaStateFragment(Scenario scenario,
      List<InteractionFragment> interactionFragments,
      org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment stateFragment) {

    InstanceRole instanceRole = EmbeddedEditorInstanceHelper.getInstanceRole(stateFragment.getTimeline());

    List<TimeLapse> filteredTimeLapses;
    if (stateFragment.getKeyword().equals(DslConstants.FUNCTION)) {
      filteredTimeLapses = scenario.getOwnedTimeLapses().stream()
          .filter(x -> x instanceof StateFragment
              && ((StateFragment) x).getRelatedAbstractFunction().getName().equals(stateFragment.getName())
              && x.getStart().getCoveredInstanceRoles().get(0).equals(instanceRole)
              && !interactionFragments.contains(((StateFragment) x).getStart()))
          .collect(Collectors.toList());
    } else {
      filteredTimeLapses = scenario.getOwnedTimeLapses().stream()
          .filter(x -> x instanceof StateFragment
              && ((StateFragment) x).getRelatedAbstractState().getName().equals(stateFragment.getName())
              && x.getStart().getCoveredInstanceRoles().get(0).equals(instanceRole)
              && !interactionFragments.contains(((StateFragment) x).getStart()))
          .collect(Collectors.toList());
    }

    if (!filteredTimeLapses.isEmpty()) {
      return (StateFragment) filteredTimeLapses.get(0);
    }
    return null;
  }

  /**
   * Return the corresponding Capella combined fragment for the given xtext element
   * 
   * @param scenario
   *          The scenario diagram
   * @param textCombinedFragment
   *          The element representing the combined fragment in the xtext editor
   * @return the corresponding Capella combined fragment or null if not found
   */
  private static CombinedFragment getCorrespondingCapellaCombinedFragment(Scenario scenario,
      org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment textCombinedFragment) {
    EList<InteractionFragment> fragments = scenario.getOwnedInteractionFragments();

    for (Iterator<InteractionFragment> iterator = fragments.iterator(); iterator.hasNext();) {
      InteractionFragment element = iterator.next();
      if (element instanceof FragmentEnd) {
        CombinedFragment candidateCombinedFragment = (CombinedFragment) ((FragmentEnd) element).getAbstractFragment();
        if (candidateCombinedFragment.getOperator().toString().equalsIgnoreCase(textCombinedFragment.getKeyword())) {          
          //check timelines
          if (haveSameTimelines(textCombinedFragment, candidateCombinedFragment)) {
            List<InteractionOperand> capellaOperands = candidateCombinedFragment.getReferencedOperands();
            if (capellaOperands.size() == textCombinedFragment.getOperands().size() + 1
                && operandsHaveSameExpressions(textCombinedFragment, capellaOperands)) {
              return candidateCombinedFragment;
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Check if the the given combined fragments have the same timelines list
   * 
   * @param textCombinedFragment
   *          The element representing the combined fragment in the xtext editor
   * @param capellaCombinedFragment
   *          Capella combined fragment
   * @return true if the combined fragments have the same timelines list, false otherwise
   */
  private static boolean haveSameTimelines(
      org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment textCombinedFragment,
      CombinedFragment capellaCombinedFragment) {
    
    EList<String> xtextTimelines = textCombinedFragment.getTimelines();    
    List<String> capellaTimelines = capellaCombinedFragment.getStart().getCoveredInstanceRoles().stream()
        .map(role -> role.getName()).collect(Collectors.toList());
    
    xtextTimelines.sort(Comparator.naturalOrder());
    capellaTimelines.sort(Comparator.naturalOrder());
    return xtextTimelines.equals(capellaTimelines);
  }

  /**
   * Check if the operands have the same expressions, one by one
   * 
   * @param textCombinedFragment
   *          The element representing the combined fragment in the xtext editor (it contains the operands)
   * @param capellaOperands
   *          List of operands to compare to
   * @return true if the operands match, false otherwise
   */
  private static boolean operandsHaveSameExpressions(
      org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment textCombinedFragment,
      List<InteractionOperand> capellaOperands) {
    
    //check expression on first operand
    InteractionOperand capellaOperand = capellaOperands.get(0);
    Operand xtextOperand;
    boolean operandsHaveSameExpressions = true;
    if (!HelperCommands.getExpressionText(capellaOperand).equals(textCombinedFragment.getExpression())) {
      operandsHaveSameExpressions = false;
    }
    
    //check expression on the rest of the operands           
    int i = 1;
    while (i < capellaOperands.size() && operandsHaveSameExpressions) {
      capellaOperand = capellaOperands.get(i);
      xtextOperand = textCombinedFragment.getOperands().get(i - 1);
      if (!HelperCommands.getExpressionText(capellaOperand).equals(xtextOperand.getExpression())) {
        operandsHaveSameExpressions = false;
      }
      i++;
    }
    return operandsHaveSameExpressions;
  }

  /**
   * Remove all messages that are in the diagram, but not in the editor. 
   * Remove all related elements (execution, interaction fragments, etc)
   * 
   * @param scenario
   *          The scenario diagram
   * @param messages
   *          List of messages in the xtext editor
   */
  private static void cleanUpMessages(Scenario scenario, EList<EObject> messages) {
    // Delete all diagram messages that don't appear in the xtext scenario

    EList<SequenceMessage> sequenceMessages = scenario.getOwnedMessages();
    List<EObject> allXtextSequenceMessages = getAllXtextSequenceMessages(messages);
    List<SequenceMessage> messagesToBeDeleted = sequenceMessages.stream()
        .filter(capellaSequenceMessage -> capellaSequenceMessage.getKind() != MessageKind.REPLY
            && !foundCapellaMessageInXText(capellaSequenceMessage, allXtextSequenceMessages))
        .collect(Collectors.toList());

    for (SequenceMessage sequenceMessage : messagesToBeDeleted) {
      // Remove message from Capella scenario, together with execution, interaction fragments and events related to this
      // message
      removeMessageFromScenario(scenario, sequenceMessage);
    }
  }
  
  /**
   * Remove all state fragments that are in the diagram, but not in the editor. 
   * Remove all related elements.
   * 
   * @param scenario
   *          The scenario diagram
   * @param xTextElements
   *          List of elements in the xtext editor
   */
  private static void cleanUpStateFragments(Scenario scenario, EList<EObject> xTextElements) {
    // Delete all diagram state fragments that don't appear in the xtext scenario
    List<EObject> allXtextStateFragments =  xTextElements.stream()
        .filter(element -> element instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment)
        .collect(Collectors.toList());

    List<TimeLapse> stateFragmentsToBeDeleted = getCapellaStateFragmentsToBeDeleted(scenario, allXtextStateFragments);
    
    for (TimeLapse timeLapse : stateFragmentsToBeDeleted) {
      removeStateFragmentFromScenario(scenario, timeLapse);
    }
  }

  /**
   * Remove all combined fragments that are in the diagram, but not in the editor. 
   * Remove all related elements.
   * 
   * @param scenario
   *          The scenario diagram
   * @param xTextElements
   *          List of elements in the xtext editor
   */
  private static void cleanUpCombinedFragments(Scenario scenario, EList<EObject> xTextElements) {
    // Delete all diagram combined fragments that don't appear in the xtext scenario
    List<EObject> allXtextCombinedFragments =  xTextElements.stream()
        .filter(element -> element instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment)
        .collect(Collectors.toList());

    List<TimeLapse> combinedFragmentsToBeDeleted = getCapellaCombinedFragmentsToBeDeleted(scenario, allXtextCombinedFragments);
    
    for (TimeLapse timeLapse : combinedFragmentsToBeDeleted) {
      removeCombinedFragmentFromScenario(scenario, timeLapse);
    }
  }

  /**
   * Return the list of Capella combined fragments to be deleted (the ones that don't have corresponding 
   * xtext elements)
   * 
   * @param scenario
   *          The scenario diagram
   * @param allXtextCombinedFragments
   *          List of all combined fragments elements in the xtext editor
   * @return the list of Capella combined fragments to be deleted
   */
  private static List<TimeLapse> getCapellaCombinedFragmentsToBeDeleted(Scenario scenario,
      List<EObject> allXtextCombinedFragments) {
    // We mark for deletion all combined fragments in scenario that don't have corresponding xtext fragments
    List<TimeLapse> capellaCombinedFragments = scenario.getOwnedTimeLapses().stream()
        .filter(timelapse -> timelapse instanceof CombinedFragment).collect(Collectors.toList());
    
    List<org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment> processedXtextCombinedFragments = 
        new ArrayList<>();
    List<TimeLapse> combinedFragmentsToBeDeleted = new ArrayList<>();
    
    for (TimeLapse timeLapse : capellaCombinedFragments) {
      if (!foundCapellaCombinedFragmentInXText(timeLapse, allXtextCombinedFragments, processedXtextCombinedFragments)) {
        combinedFragmentsToBeDeleted.add(timeLapse);
      }
    }
    
    return combinedFragmentsToBeDeleted;
  }

  /**
   * Search for a corresponding combined fragment in xtext editor, for a given Capella combined fragment
   * 
   * @param timeLapse
   *          Timelapse of a Capella combined fragment
   * @param allXtextCombinedFragments
   *          List of all combined fragments elements in the xtext editor
   * @param processedXtextCombinedFragments
   *          List of already considered xtext combined fragments
   * @return true if an xtext combined fragment is found, false otherwise
   */
  private static boolean foundCapellaCombinedFragmentInXText(TimeLapse timeLapse,
      List<EObject> allXtextCombinedFragments,
      List<org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment> processedXtextCombinedFragments) {
    for (EObject xtextCombinedFragment : allXtextCombinedFragments) {
      if (!processedXtextCombinedFragments.contains(xtextCombinedFragment) 
          && isSameCombinedFragment(xtextCombinedFragment, timeLapse)) {
        processedXtextCombinedFragments.add((org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment) xtextCombinedFragment);
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the given fragment is the same with the one corresponding to the given timelapse
   * 
   * @param fragment
   *          Capella combined fragment
   * @param timeLapse
   *          Timelapse of a Capella combined fragment
   * @return true if the two combined fragments match, false otherwise
   */
  private static boolean isSameCombinedFragment(EObject fragment, TimeLapse timeLapse) {
    if (!(fragment instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment)) {
      return false;
    }
    org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment xtextCombinedFragment = 
        (org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment) fragment;   
    CombinedFragment correspondingCapellaFragment = getCorrespondingCapellaCombinedFragment((Scenario) timeLapse.eContainer(), xtextCombinedFragment);
    
    return timeLapse.equals(correspondingCapellaFragment);
  }

  /**
   * Return the list of Capella state fragments to be deleted (the ones that don't have corresponding 
   * xtext elements)
   * 
   * @param scenario
   *          The scenario diagram
   * @param allXtextStateFragments
   *          List of all state fragments elements in the xtext editor
   * @return the list of Capella state fragments to be deleted
   */
  private static List<TimeLapse> getCapellaStateFragmentsToBeDeleted(Scenario scenario,
      List<EObject> allXtextStateFragments) {
    // We mark for deletion all state fragments in scenario that don't have corresponding xtext fragments
    List<TimeLapse> capellaStateFragments = scenario.getOwnedTimeLapses().stream()
        .filter(timelapse -> timelapse instanceof StateFragment).collect(Collectors.toList());
    
    List<org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment> processedXtextStateFragments = 
        new ArrayList<>();
    List<TimeLapse> stateFragmentsToBeDeleted = new ArrayList<>();
    
    for (TimeLapse timeLapse : capellaStateFragments) {
      if (!foundCapellaStateFragmentInXText(timeLapse, allXtextStateFragments, processedXtextStateFragments)) {
        stateFragmentsToBeDeleted.add(timeLapse);
      }
    }
    
    return stateFragmentsToBeDeleted;
  }

  /**
   * Remove the given sequence message from scenario. Remove all related elements.
   * 
   * @param scenario
   *          The scenario diagram
   * @param sequenceMessage
   *          Capella sequence message to be removed
   */
  private static void removeMessageFromScenario(Scenario scenario, SequenceMessage sequenceMessage) {
    // Remove execution - time lapse
    Execution execution = null;
    MessageEnd re = sequenceMessage.getReceivingEnd();
    for (TimeLapse tl : scenario.getOwnedTimeLapses()) {
      if (tl instanceof Execution) {
        Execution exec = (Execution) tl;
        if (exec.getStart() != null && exec.getStart().equals(re)) {
          execution = exec;
        }
      }
    }
    scenario.getOwnedTimeLapses().remove(execution);

    // Remove interaction fragments for sending end, receiving end and execution end
    MessageEnd sendingEnd = sequenceMessage.getSendingEnd();
    MessageEnd receivingEnd = sequenceMessage.getReceivingEnd();
    InteractionFragment executionEnd = execution != null ? execution.getFinish() : null;
    scenario.getOwnedInteractionFragments().remove(sendingEnd);
    scenario.getOwnedInteractionFragments().remove(receivingEnd);
    scenario.getOwnedInteractionFragments().remove(executionEnd);

    // Remove events
    Event eventSendOp = sendingEnd.getEvent();
    Event eventReveivOp = receivingEnd.getEvent();
    scenario.getOwnedEvents().remove(eventSendOp);
    scenario.getOwnedEvents().remove(eventReveivOp);

    if (sequenceMessage.getKind() != MessageKind.REPLY && !ScenarioExt.hasReply(sequenceMessage)) {
      // Remove execution event for normal sequence message
      Event executionEvent = executionEnd instanceof ExecutionEnd
          ? (Event) ExecutionEndExt.getOperation((ExecutionEnd) executionEnd)
          : null;
      scenario.getOwnedEvents().remove(executionEvent);
    }

    // Remove sequence message
    scenario.getOwnedMessages().remove(sequenceMessage);
  }
  
  /**
   * Remove a state fragment from scenario. Remove all related elements.
   * 
   * @param scenario
   *          The scenario diagram
   * @param timeLapse
   *          The timelapse of the state fragment
   */
  private static void removeStateFragmentFromScenario(Scenario scenario, TimeLapse timeLapse) {    
    StateFragment stateFragment = (StateFragment) timeLapse;
    
    // Remove state fragment
    scenario.getOwnedTimeLapses().remove(timeLapse);
    
    // Remove interaction fragments
    scenario.getOwnedInteractionFragments().removeAll(Arrays.asList(stateFragment.getStart(), stateFragment.getFinish()));
  }

  /**
   * Remove a combined fragment from scenario. Remove all related elements.
   * 
   * @param scenario
   *          The scenario diagram
   * @param timeLapse
   *          The timelapse of the combined fragment
   */
  private static void removeCombinedFragmentFromScenario(Scenario scenario, TimeLapse timeLapse) {
    CombinedFragment combinedFragment = (CombinedFragment) timeLapse;
    
    // Remove combined fragment
    scenario.getOwnedTimeLapses().remove(timeLapse);
    
    // Remove interaction fragments (start, finish, operands)
    scenario.getOwnedInteractionFragments().removeAll(Arrays.asList(combinedFragment.getStart(), combinedFragment.getFinish(), 
        combinedFragment.getReferencedOperands()));
  }

  /**
   * Check if a Capella sequence message has a correspondent in xtext scenario
   * 
   * @param capellaSequenceMessage
   *          Capella sequence message
   * @param allXtextSequenceMessages
   *          List of all sequence messages in the xtext scenario
   * @return true if a corresponding xtext message is found, false otherwise
   */
  private static boolean foundCapellaMessageInXText(SequenceMessage capellaSequenceMessage,
      List<EObject> allXtextSequenceMessages) {
    for (EObject message : allXtextSequenceMessages) {
      if (isSameMessage(message, capellaSequenceMessage)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if a Capella state fragment has a correspondent in xtext scenario
   * 
   * @param timelapse
   *          Timelapse of the state fragment
   * @param allXtextStateFragments
   *          List of all sequence messages in the xtext scenario
   * @param processedXtextStateFragments
   *          List of already considered xtext state fragments
   * @return true if a corresponding state fragment is found, false otherwise
   */
  private static boolean foundCapellaStateFragmentInXText(TimeLapse timelapse,
      List<EObject> allXtextStateFragments, List<org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment> processedXtextStateFragments) {
    for (EObject stateFragment : allXtextStateFragments) {
      if (!processedXtextStateFragments.contains(stateFragment) 
          && isSameStateFragment(stateFragment, timelapse)) {
        processedXtextStateFragments.add((org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment) stateFragment);
        return true;
      }
    }
    return false;
  }

  /**
   * Return the list of all xtext sequence messages. The search is done recursively when combined fragments are encountered
   * 
   * @param textMessages
   *          List of messages in the xtext editor
   * @return the list of all xtext sequence messages
   */
  private static List<EObject> getAllXtextSequenceMessages(EList<EObject> textMessages) {
    ArrayList<EObject> xtextSequenceMessages = new ArrayList<>();
    for (EObject element : textMessages) {
      // SequenceMessage -> add it
      if (element instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.Message) {
        xtextSequenceMessages.add(element);
      }
      // CombinedFragment -> go inside and find all sequence messages at all levels
      if (element instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment) {
        org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment combinedFragmentElement = (org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment) element;

        // get sequence messages from the first operand (first block, actually)
        xtextSequenceMessages
            .addAll(getAllXtextSequenceMessages(combinedFragmentElement.getBlock().getBlockElements()));

        // get sequence messages from the rest of the operands
        for (EObject operand : combinedFragmentElement.getOperands()) {
          xtextSequenceMessages.addAll(getAllXtextSequenceMessages(((Operand) operand).getBlock().getBlockElements()));
        }
      }
    }

    return xtextSequenceMessages;
  }

  /**
   * Check if the two messages match
   * 
   * @param xtextElement
   *          Message in the xtext editor
   * @param seqMessage
   *          Capella sequence message
   * @return true if the two messages match, false otherwise
   */
  private static boolean isSameMessage(EObject xtextElement, SequenceMessage seqMessage) {
    if (!(xtextElement instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.Message)) {
      return false;
    }
    MessageEnd sendingEnd = null;
    MessageEnd receivingEnd = null;
    String source = null;
    String target = null;
    String xtextMessageName = null;
    String capellaMessageName = seqMessage.getName();
    
    if (xtextElement instanceof SequenceMessageType) {
      org.polarsys.capella.scenario.editor.dsl.textualScenario.SequenceMessageType message = 
          (org.polarsys.capella.scenario.editor.dsl.textualScenario.SequenceMessageType) xtextElement;
      sendingEnd = seqMessage.getSendingEnd();
      receivingEnd = seqMessage.getReceivingEnd();
      source = message.getSource();
      target = message.getTarget();
      xtextMessageName = message.getName();
    } else if (xtextElement instanceof ArmTimerMessage) {
      org.polarsys.capella.scenario.editor.dsl.textualScenario.ArmTimerMessage armTimerMessage = 
          (org.polarsys.capella.scenario.editor.dsl.textualScenario.ArmTimerMessage) xtextElement;
      sendingEnd = seqMessage.getSendingEnd();
      receivingEnd = seqMessage.getReceivingEnd();
      source = armTimerMessage.getParticipant();
      target = armTimerMessage.getParticipant();
      xtextMessageName = armTimerMessage.getName();
    }
    
    return sendingEnd == null ? false : 
      isSameMessage(source, target, sendingEnd, receivingEnd, xtextMessageName, capellaMessageName);
  }

  /**
   * Check if the two messages match
   * 
   * @param source
   *          Source of the xtext message
   * @param target
   *          Target of the xtext message
   * @param sendingEnd
   *          Sending end of the Capella message
   * @param receivingEnd
   *          Receiving end of the Capella message
   * @param messageName
   *          Name of the xtext message
   * @param capellaMessageName
   *          Name of the Capella message
   * @return true if the two messages match, false otherwise
   */
  private static boolean isSameMessage(String source, String target, MessageEnd sendingEnd, MessageEnd receivingEnd,
      String messageName, String capellaMessageName) {
    if (!sendingEnd.getCoveredInstanceRoles().isEmpty()
        && source.equals(sendingEnd.getCoveredInstanceRoles().get(0).getName())
        && !receivingEnd.getCoveredInstanceRoles().isEmpty()
        && target.equals(receivingEnd.getCoveredInstanceRoles().get(0).getName())
        && messageName.equals(capellaMessageName)) {
      return true;
    }
    return false;
  }

  /**
   * Check if the two state fragments match
   * 
   * @param fragment
   *          xtext state message
   * @param timelapse
   *          Timelapse of a Capella state fragment
   * @return true if the two state fragments match, false otherwise
   */
  private static boolean isSameStateFragment(EObject fragment, TimeLapse timelapse) {
    if (!(fragment instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment)) {
      return false;
    }
    org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment stateFragment = 
        (org.polarsys.capella.scenario.editor.dsl.textualScenario.StateFragment) fragment;
    StateFragment capellaStateFragment = (StateFragment) timelapse;
       
    return (stateFragment.getTimeline().equals(capellaStateFragment.getStart().getCoveredInstanceRoles().get(0).getName())
        && stateFragment.getKeyword().equals(EmbeddedEditorInstanceHelper.getStateFragmentType(capellaStateFragment))
        && stateFragment.getName().equals(EmbeddedEditorInstanceHelper.getStateFragmentName(capellaStateFragment)));
  }

  /**
   * Creates a Capella sequence message, with all its elements.
   * 
   * @param scenario
   *          The scenario diagram
   * @param source
   *          Source of the Capella sequence message
   * @param target
   *          Target of the Capella sequence message
   * @param seqMessage
   *          The element representing the sequence message in the xtext editor
   * @return the newly created Capella sequence message
   */
  private static SequenceMessage createCapellaSequenceMessage(Scenario scenario, InstanceRole source,
      InstanceRole target, Message seqMessage) {
    return createCapellaSequenceMessage(scenario, source, target, seqMessage, false);
  }

  /*
   * Create a capella sequence message and set all data needed by a capella sequence message : sending end, receiving
   * end, execution etc
   * @param scenario
   *          The scenario diagram
   * @param source
   *          Source of the Capella sequence message
   * @param target
   *          Target of the Capella sequence message
   * @param seqMessage
   *          The element representing the sequence message in the xtext editor 
   * @param isReplyMessage
   *          True if this is the message representing the return branch of a sequence message with return branch
   * @return the newly created Capella sequence message
   */
  private static SequenceMessage createCapellaSequenceMessage(Scenario scenario, InstanceRole source,
      InstanceRole target, Message seqMessage,
      boolean isReplyMessage) {
    // create Capella SequenceMessage
    SequenceMessage sequenceMessage = InteractionFactory.eINSTANCE.createSequenceMessage();
    sequenceMessage.setName(seqMessage.getName());
    sequenceMessage.setKind(getSequenceMessageKind(seqMessage, isReplyMessage));

    // sending end
    MessageEnd sendingEnd = InteractionFactory.eINSTANCE.createMessageEnd();
    sendingEnd.getCoveredInstanceRoles().add(source);
    sequenceMessage.setSendingEnd(sendingEnd);
    scenario.getOwnedInteractionFragments().add(sendingEnd);

    // receiving end
    MessageEnd receivingEnd = InteractionFactory.eINSTANCE.createMessageEnd();
    receivingEnd.getCoveredInstanceRoles().add(target);
    sequenceMessage.setReceivingEnd(receivingEnd);
    scenario.getOwnedInteractionFragments().add(receivingEnd);

    // execution end - CREATE and DELETE messages don't have an execution
    if (!isReplyMessage && 
        (seqMessage instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.SequenceMessage ||
         seqMessage instanceof ArmTimerMessage)) {
      ExecutionEnd executionEnd = InteractionFactory.eINSTANCE.createExecutionEnd();
      if (!hasReturn(seqMessage)) {
        // do not add this execution end to the interaction fragments list, as we will add the sending end of the reply
        // message
        scenario.getOwnedInteractionFragments().add(executionEnd);
      }
      executionEnd.getCoveredInstanceRoles().add(receivingEnd.getCoveredInstanceRoles().get(0));

      // execution
      Execution execution = InteractionFactory.eINSTANCE.createExecution();
      execution.setFinish(executionEnd);
      execution.setStart(receivingEnd);
      scenario.getOwnedTimeLapses().add(execution);

      // execution event
      ExecutionEvent executionEvent = InteractionFactory.eINSTANCE.createExecutionEvent();
      executionEnd.setEvent(executionEvent);
      scenario.getOwnedEvents().add(executionEvent);
    }

    // EventSentOperation
    EventSentOperation eventSentOperation = InteractionFactory.eINSTANCE.createEventSentOperation();
    scenario.getOwnedEvents().add(eventSentOperation);
    sendingEnd.setEvent(eventSentOperation);

    // EventReceiptOperation
    EventReceiptOperation eventRecvOperation = InteractionFactory.eINSTANCE.createEventReceiptOperation();
    scenario.getOwnedEvents().add(eventRecvOperation);
    receivingEnd.setEvent(eventRecvOperation);

    // get operation by name from the list of available exchanges
    List<AbstractEventOperation> exchanges = null;
    if (isReplyMessage) {
      exchanges = EmbeddedEditorInstanceHelper.getExchangeMessages(target.getName(), source.getName());
    } else {
      exchanges = EmbeddedEditorInstanceHelper.getExchangeMessages(source.getName(), target.getName());
    }
    
    if (EmbeddedEditorInstanceHelper.isInterfaceScenario()) {
      exchanges = exchanges.stream()
          .filter(ex -> ((ExchangeItemAllocation) ex).getAllocatedItem().getName().equals(seqMessage.getName()))
          .collect(Collectors.toList());
    } else {
      exchanges = exchanges.stream().filter(ex -> ex.getName().equals(seqMessage.getName()))
          .collect(Collectors.toList());
    }
    if (!exchanges.isEmpty()) {
      eventRecvOperation.setOperation((AbstractEventOperation) exchanges.get(0));
      eventSentOperation.setOperation((AbstractEventOperation) exchanges.get(0));
    }

    return sequenceMessage;
  }

  /**
   * Return the kind of sequence message
   * 
   * @param seqMessage
   *          The element representing the sequence message in the xtext editor
   * @param isReplyMessage
   *          True if this is the message representing the return branch of a sequence message with return branch
   * @return the kind of sequence message
   */
  private static MessageKind getSequenceMessageKind(Message seqMessage, boolean isReplyMessage) {
    if (seqMessage instanceof org.polarsys.capella.scenario.editor.dsl.textualScenario.SequenceMessage) {
      return isReplyMessage ? MessageKind.REPLY
          : ((org.polarsys.capella.scenario.editor.dsl.textualScenario.SequenceMessage) seqMessage).getReturn() != null ? 
              MessageKind.SYNCHRONOUS_CALL : MessageKind.ASYNCHRONOUS_CALL; 
    }
    if (seqMessage instanceof CreateMessage) {
      return MessageKind.CREATE;
    } 
    if (seqMessage instanceof DeleteMessage) {
      return MessageKind.DELETE;
    }
    if (seqMessage instanceof ArmTimerMessage) {
      return MessageKind.TIMER;
    }
    return MessageKind.UNSET;
  }

  /**
   * Move the interaction fragment representing the execution end to the correct position in the interaction
   * fragments list, according to the xtext scenario
   * 
   * @param fragments
   *          The new list of ordered interaction fragments that will be used to update the Capella diagram
   *          according to the xtext scenario
   * @param instanceRole
   *          Timeline in the deactivation message
   * @param executionEndsToProcess
   *          List of execution ends that have to be processed (moved on the correct order in the list of
   *          interaction fragments.
   * @param sequenceMessages
   *          The new list of Capella sequence messages that will be used to update the Capella diagram
   *          according to the xtext scenario
   */
  private static void doDeactivationSequenceMessageForReorder(List<InteractionFragment> fragments,
      InstanceRole instanceRole, List<InteractionFragment> executionEndsToProcess,
      List<SequenceMessage> sequenceMessages) {
    InteractionFragment executionEnd = getLatestExecutionEndOnTimeline(instanceRole, executionEndsToProcess);

    SequenceMessage sequenceMessage = null;
    SequenceMessage oppositeSequenceMessage = null;
    if (executionEnd != null) {
      if (executionEnd instanceof ExecutionEnd) {
        sequenceMessage = ExecutionEndExt.getMessage((ExecutionEnd) executionEnd);
      } else {
        sequenceMessage = ((MessageEnd) executionEnd).getMessage();
        oppositeSequenceMessage = getOppositeSequenceMessage(sequenceMessage, fragments);
      }

      if (oppositeSequenceMessage != null) {
        // This message has return branch, so instead of execution end we must add the sending and receiving end of its
        // reply message
        fragments.add(sequenceMessage.getSendingEnd());
        fragments.add(sequenceMessage.getReceivingEnd());
        sequenceMessages.add(sequenceMessage);
      } else {
        fragments.add(executionEnd);
      }
    }

    // remove executionEnd from the processing list
    executionEndsToProcess.remove(executionEnd);
  }

  /**
   * Return the latest execution on the given timeline
   * 
   * @param instanceRole
   *          Timeline for which we search the latest execution
   * @param executionEndsToProcess
   *          List of execution ends that have to be processed (moved on the correct order in the list of
   *          interaction fragments.
   * @return the execution end for the latest execution on the given timeline
   */
  private static InteractionFragment getLatestExecutionEndOnTimeline(InstanceRole instanceRole,
      List<InteractionFragment> executionEndsToProcess) {
    return executionEndsToProcess.stream()
        .filter(e -> e.getCoveredInstanceRoles().get(0).getName().equals(instanceRole.getName()))
        .reduce((first, second) -> second).orElse(null);
  }

  /**
   * Create a Capella combined fragment with all its elements.
   * 
   * @param scenario
   *          The scenario diagram
   * @param xtextCombinedFragment
   *          xtext combined fragment
   * @param lastInteractionFragment
   *          The last interaction fragment before the given combined fragment. If null, the combined fragment
   *          will be the first object in the Capella diagram
   */
  private static void createCapellaCombinedFragmentBlock(Scenario scenario,
      org.polarsys.capella.scenario.editor.dsl.textualScenario.CombinedFragment xtextCombinedFragment,
      InteractionFragment lastInteractionFragment) {

    // generate CombinedFragment, FramentEnd, FragmentEnd, IntreationOperands from xtext combined fragment
    FragmentEnd start = InteractionFactory.eINSTANCE.createFragmentEnd();
    FragmentEnd finish = InteractionFactory.eINSTANCE.createFragmentEnd();

    xtextCombinedFragment.getTimelines().forEach(timeline -> {
      InstanceRole instanceRole = EmbeddedEditorInstanceHelper.getInstanceRole(timeline);
      start.getCoveredInstanceRoles().add(instanceRole);
      finish.getCoveredInstanceRoles().add(instanceRole);
    });

    start.setName("start");
    finish.setName("end");

    scenario.getOwnedInteractionFragments().add(start);
    scenario.getOwnedInteractionFragments().add(finish);

    if (lastInteractionFragment != null)
      ScenarioExt.moveEndOnScenario(start, lastInteractionFragment);
    else
      ScenarioExt.moveEndOnBeginingOfScenario(start);

    CombinedFragment combinedFragment = createCombinedFragment(start, finish, InteractionOperatorKind.getByName(xtextCombinedFragment.getKeyword().toUpperCase()));
    scenario.getOwnedTimeLapses().add(combinedFragment);

    Block firstBlock = xtextCombinedFragment.getBlock();
    if (firstBlock != null) {
      InteractionOperand operand = createInteractionOperand(start.getCoveredInstanceRoles(), xtextCombinedFragment.getExpression());
      combinedFragment.getReferencedOperands().add(operand);
      scenario.getOwnedInteractionFragments().add(operand);
      ScenarioExt.moveEndOnScenario(operand, start);

      editElements(scenario, firstBlock.getBlockElements());

      InteractionOperand prevEnd = operand;
      for (Operand operandBlock : xtextCombinedFragment.getOperands()) {
        InteractionOperand operand2 = createInteractionOperand(start.getCoveredInstanceRoles(),
            operandBlock.getExpression());
        combinedFragment.getReferencedOperands().add(operand2);
        scenario.getOwnedInteractionFragments().add(operand2);
        ScenarioExt.moveEndOnScenario(operand2, prevEnd);
        prevEnd = operand2;

        editElements(scenario, operandBlock.getBlock().getBlockElements());
      }
    }
  }

  /**
   * Create a Capella interaction operand with all its elements.
   * 
   * @param coveredInstanceRoles
   *          List of timelines covered by the interaction operand
   * @param condition
   *          The expression for the operand
   * @return the newly created interaction operand
   */
  private static InteractionOperand createInteractionOperand(EList<InstanceRole> coveredInstanceRoles, String condition) {
    InteractionOperand operand = InteractionFactory.eINSTANCE.createInteractionOperand();
    operand.setName("operand");
    operand.getCoveredInstanceRoles().addAll(coveredInstanceRoles);

    Constraint constraint = CapellacoreFactory.eINSTANCE.createConstraint();
    OpaqueExpression expression = DatavalueFactory.eINSTANCE.createOpaqueExpression();

    constraint.setOwnedSpecification(expression);
    operand.getOwnedConstraints().add(constraint);
    CreationHelper.performContributionCommands(constraint, operand);

    expression.getLanguages().add(ConstraintExt.OPAQUE_EXPRESSION_LINKED_TEXT);
    expression.getBodies().add(condition);

    operand.setGuard(constraint);
    return operand;
  }

  /**
   * Create a Capella combined fragment with all its elements.
   * 
   * @param start
   *          Interaction fragment representing the start of the combined fragment
   * @param finish
   *          Interaction fragment representing the end of the combined fragment
   * @param kind
   *          The kind of combined fragment
   * @return the newly created combined fragment
   */
  private static CombinedFragment createCombinedFragment(InteractionFragment start, InteractionFragment finish,
      InteractionOperatorKind kind) {
    CombinedFragment combinedFragment = InteractionFactory.eINSTANCE.createCombinedFragment();
    combinedFragment.setOperator(kind);
    combinedFragment.setStart(start);
    combinedFragment.setFinish(finish);
    combinedFragment.setName("combined fragment");

    return combinedFragment;
  }

  /**
   * Return the 'calling' or 'reply' branch related to the given sequence message.
   * 
   * @param sequenceMessage
   *          Capella sequence message. It can be either a sequence message of the return branch of a sequence message
   * @param fragments
   *          List of interaction fragments
   * @return the opposite message
   */
  public static SequenceMessage getOppositeSequenceMessage(SequenceMessage sequenceMessage,
      List<InteractionFragment> fragments) {

    boolean flag = false;
    List<SequenceMessage> setPortionMessage = new ArrayList<>();
    Deque<SequenceMessage> messagesStack = new ArrayDeque<>();

    /** On messages of type 'destroy' there is no processing */
    if (sequenceMessage != null && !sequenceMessage.getKind().equals(MessageKind.CREATE)
        && !sequenceMessage.getKind().equals(MessageKind.DELETE)
        && !sequenceMessage.getKind().equals(MessageKind.ASYNCHRONOUS_CALL)) {
      List<MessageEnd> ownedMsgEnd = new ArrayList<>();

      for (InteractionFragment abs : fragments) {
        if (abs instanceof MessageEnd) {
          ownedMsgEnd.add((MessageEnd) abs);
        }
      }

      if (ownedMsgEnd != null) {
        if (sequenceMessage.getKind().equals(MessageKind.REPLY)) {
          /** If this is a REPLY message => the CALLING branch is present in the upper portion of the messages */
          flag = false;
          for (Iterator<MessageEnd> it = ownedMsgEnd.iterator(); it.hasNext() && !flag;) {
            MessageEnd msgEnd = it.next();
            if (msgEnd != null) {
              SequenceMessage msg = msgEnd.getMessage();
              if (msg != null) {
                if (!msg.equals(sequenceMessage)) {
                  setPortionMessage.add(msg);
                } else {
                  flag = true;
                }
              }
            }
          }
          /** Invert sequence messages order */
          setPortionMessage = SequenceMessageExt.reverse(setPortionMessage);
        } else {
          /** If this is a CALLING message => The REPLY branch is present in the upper portion of the messages */
          flag = false;
          for (Iterator<MessageEnd> it = ownedMsgEnd.iterator(); it.hasNext();) {
            MessageEnd msgEnd = it.next();
            if (msgEnd != null) {
              SequenceMessage msg = msgEnd.getMessage();
              if (msg != null) {
                if (flag) {
                  setPortionMessage.add(msg);
                } else if (msg.equals(sequenceMessage)) {
                  flag = true;
                }
              }
            }
          }
        }

        for (SequenceMessage msg : setPortionMessage) {
          if (!msg.getKind().equals(MessageKind.CREATE) && !msg.getKind().equals(MessageKind.DELETE)
              && !msg.getKind().equals(MessageKind.ASYNCHRONOUS_CALL)) {
            if (sequenceMessage.getKind().equals(MessageKind.REPLY)) {
              /**
               * Treatment: research branch 'toGo' If current message type return: Pushes current message else if stack
               * empty: branch the found 'toGo' else pops the last message and return.
               */
              if (msg.getKind().equals(MessageKind.REPLY)) {
                messagesStack.push(msg);
              } else {
                if (messagesStack.isEmpty()) {
                  return msg;
                }
                messagesStack.pop();
              }
            } else {
              /**
               * Treatment: research branch 'toGo' If current message type is return and empty stack: Branch return
               * found. * If current message type is return and non-empty stack: Pops last message 'toGo' else the
               * current message is stacked
               */
              if (msg.getKind().equals(MessageKind.REPLY)) {
                if (messagesStack.isEmpty()) {
                  return msg;
                }
                messagesStack.pop();
              } else {
                messagesStack.push(msg);
              }
            }
          }
        }
      }
    }
    return null;
  }
}
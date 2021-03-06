/*******************************************************************************
 * Copyright (c) 2020 THALES GLOBAL SERVICES.
 *  
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License 2.0 which is available at
 *  http://www.eclipse.org/legal/epl-2.0
 *  
 *  SPDX-License-Identifier: EPL-2.0
 *  
 *  Contributors:
 *     Thales - initial API and implementation
 ******************************************************************************/
/**
 * generated by Xtext 2.18.0.M3
 */
package org.polarsys.capella.scenario.editor.dsl.textualScenario.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

import org.polarsys.capella.scenario.editor.dsl.textualScenario.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class TextualScenarioFactoryImpl extends EFactoryImpl implements TextualScenarioFactory
{
  /**
   * Creates the default factory implementation.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static TextualScenarioFactory init()
  {
    try
    {
      TextualScenarioFactory theTextualScenarioFactory = (TextualScenarioFactory)EPackage.Registry.INSTANCE.getEFactory(TextualScenarioPackage.eNS_URI);
      if (theTextualScenarioFactory != null)
      {
        return theTextualScenarioFactory;
      }
    }
    catch (Exception exception)
    {
      EcorePlugin.INSTANCE.log(exception);
    }
    return new TextualScenarioFactoryImpl();
  }

  /**
   * Creates an instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public TextualScenarioFactoryImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public EObject create(EClass eClass)
  {
    switch (eClass.getClassifierID())
    {
      case TextualScenarioPackage.MODEL: return createModel();
      case TextualScenarioPackage.PARTICIPANT: return createParticipant();
      case TextualScenarioPackage.GENERIC_COMPONENT: return createGenericComponent();
      case TextualScenarioPackage.GENERIC_FUNCTION: return createGenericFunction();
      case TextualScenarioPackage.ACTOR: return createActor();
      case TextualScenarioPackage.COMPONENT: return createComponent();
      case TextualScenarioPackage.CONFIGURATION_ITEM: return createConfigurationItem();
      case TextualScenarioPackage.FUNCTION: return createFunction();
      case TextualScenarioPackage.ACTIVITY: return createActivity();
      case TextualScenarioPackage.ENTITY: return createEntity();
      case TextualScenarioPackage.ROLE: return createRole();
      case TextualScenarioPackage.MESSAGE: return createMessage();
      case TextualScenarioPackage.SEQUENCE_MESSAGE_TYPE: return createSequenceMessageType();
      case TextualScenarioPackage.SEQUENCE_MESSAGE: return createSequenceMessage();
      case TextualScenarioPackage.CREATE_MESSAGE: return createCreateMessage();
      case TextualScenarioPackage.DELETE_MESSAGE: return createDeleteMessage();
      case TextualScenarioPackage.ARM_TIMER_MESSAGE: return createArmTimerMessage();
      case TextualScenarioPackage.PARTICIPANT_DEACTIVATION: return createParticipantDeactivation();
      case TextualScenarioPackage.REFERENCE: return createReference();
      case TextualScenarioPackage.COMBINED_FRAGMENT: return createCombinedFragment();
      case TextualScenarioPackage.OPERAND: return createOperand();
      case TextualScenarioPackage.BLOCK: return createBlock();
      case TextualScenarioPackage.STATE_FRAGMENT: return createStateFragment();
      default:
        throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Model createModel()
  {
    ModelImpl model = new ModelImpl();
    return model;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Participant createParticipant()
  {
    ParticipantImpl participant = new ParticipantImpl();
    return participant;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public GenericComponent createGenericComponent()
  {
    GenericComponentImpl genericComponent = new GenericComponentImpl();
    return genericComponent;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public GenericFunction createGenericFunction()
  {
    GenericFunctionImpl genericFunction = new GenericFunctionImpl();
    return genericFunction;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Actor createActor()
  {
    ActorImpl actor = new ActorImpl();
    return actor;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Component createComponent()
  {
    ComponentImpl component = new ComponentImpl();
    return component;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public ConfigurationItem createConfigurationItem()
  {
    ConfigurationItemImpl configurationItem = new ConfigurationItemImpl();
    return configurationItem;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Function createFunction()
  {
    FunctionImpl function = new FunctionImpl();
    return function;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Activity createActivity()
  {
    ActivityImpl activity = new ActivityImpl();
    return activity;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Entity createEntity()
  {
    EntityImpl entity = new EntityImpl();
    return entity;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Role createRole()
  {
    RoleImpl role = new RoleImpl();
    return role;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Message createMessage()
  {
    MessageImpl message = new MessageImpl();
    return message;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public SequenceMessageType createSequenceMessageType()
  {
    SequenceMessageTypeImpl sequenceMessageType = new SequenceMessageTypeImpl();
    return sequenceMessageType;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public SequenceMessage createSequenceMessage()
  {
    SequenceMessageImpl sequenceMessage = new SequenceMessageImpl();
    return sequenceMessage;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public CreateMessage createCreateMessage()
  {
    CreateMessageImpl createMessage = new CreateMessageImpl();
    return createMessage;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public DeleteMessage createDeleteMessage()
  {
    DeleteMessageImpl deleteMessage = new DeleteMessageImpl();
    return deleteMessage;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public ArmTimerMessage createArmTimerMessage()
  {
    ArmTimerMessageImpl armTimerMessage = new ArmTimerMessageImpl();
    return armTimerMessage;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public ParticipantDeactivation createParticipantDeactivation()
  {
    ParticipantDeactivationImpl participantDeactivation = new ParticipantDeactivationImpl();
    return participantDeactivation;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Reference createReference()
  {
    ReferenceImpl reference = new ReferenceImpl();
    return reference;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public CombinedFragment createCombinedFragment()
  {
    CombinedFragmentImpl combinedFragment = new CombinedFragmentImpl();
    return combinedFragment;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Operand createOperand()
  {
    OperandImpl operand = new OperandImpl();
    return operand;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Block createBlock()
  {
    BlockImpl block = new BlockImpl();
    return block;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public StateFragment createStateFragment()
  {
    StateFragmentImpl stateFragment = new StateFragmentImpl();
    return stateFragment;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public TextualScenarioPackage getTextualScenarioPackage()
  {
    return (TextualScenarioPackage)getEPackage();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @deprecated
   * @generated
   */
  @Deprecated
  public static TextualScenarioPackage getPackage()
  {
    return TextualScenarioPackage.eINSTANCE;
  }

} //TextualScenarioFactoryImpl

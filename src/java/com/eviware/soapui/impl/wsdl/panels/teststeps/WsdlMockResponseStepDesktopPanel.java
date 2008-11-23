/*
 *  soapUI Pro, copyright (C) 2007-2008 eviware software ab 
 */

package com.eviware.soapui.impl.wsdl.panels.teststeps;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.support.components.ModelItemXmlEditor;
import com.eviware.soapui.impl.wsdl.mock.WsdlMockResponse;
import com.eviware.soapui.impl.wsdl.mock.WsdlMockResult;
import com.eviware.soapui.impl.wsdl.panels.mockoperation.AbstractWsdlMockResponseDesktopPanel;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMockResponseTestStep;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.support.TestRunListenerAdapter;
import com.eviware.soapui.model.testsuite.*;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.monitor.support.TestMonitorListenerAdapter;
import com.eviware.soapui.support.DocumentListenerAdapter;
import com.eviware.soapui.support.ModelItemPropertyEditorModel;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JInspectorPanelFactory;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.xml.XmlUtils;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.util.Date;

public class WsdlMockResponseStepDesktopPanel extends AbstractWsdlMockResponseDesktopPanel<WsdlMockResponseTestStep, WsdlMockResponse>
{
   private JTextArea logArea;
   private AssertionsPanel assertionsPanel;
   private JTextField portField;
   private JTextField pathField;
   private InternalTestRunListener testRunListener;
   private InternalTestMonitorListener testMonitorListener = new InternalTestMonitorListener();
   private JInspectorPanel inspectorPanel;
   private JComponentInspector<JComponent> assertionInspector;
   private JComponentInspector<JComponent> logInspector;
   private ModelItemPropertyEditorModel<WsdlMockResponseTestStep> queryEditorModel;
   private ModelItemPropertyEditorModel<WsdlMockResponseTestStep> matchEditorModel;

   public WsdlMockResponseStepDesktopPanel( WsdlMockResponseTestStep mockResponseStep )
   {
      super( mockResponseStep );
      init( mockResponseStep.getMockResponse() );

      testRunListener = new InternalTestRunListener();
      mockResponseStep.getTestCase().addTestRunListener( testRunListener );

      SoapUI.getTestMonitor().addTestMonitorListener( testMonitorListener );
      setEnabled( !SoapUI.getTestMonitor().hasRunningTest( mockResponseStep.getTestCase() ) );
   }

   @Override
   protected JComponent buildContent()
   {
      inspectorPanel = JInspectorPanelFactory.build( super.buildContent() );

      assertionsPanel = buildAssertionsPanel();

      assertionInspector = new JComponentInspector<JComponent>( assertionsPanel, "Assertions ("
              + getModelItem().getAssertionCount() + ")", "Assertions for this Test Request", true );

      inspectorPanel.addInspector( assertionInspector );

      logInspector = new JComponentInspector<JComponent>( buildLogPanel(), "Request Log (0)", "Log of requests", true );
      inspectorPanel.addInspector( logInspector );

      inspectorPanel.addInspector( new JComponentInspector<JComponent>( buildQueryMatchPanel(), "Query/Match",
              "Query/Match configuration", true ) );

      inspectorPanel.setDefaultDividerLocation( 0.6F );
      inspectorPanel.setCurrentInspector( "Assertions" );

      updateStatusIcon();

      return inspectorPanel.getComponent();
   }

    private void updateStatusIcon()
   {
      Assertable.AssertionStatus status = getModelItem().getAssertionStatus();
      switch( status )
      {
         case FAILED:
         {
            assertionInspector.setIcon( UISupport.createImageIcon( "/failed_assertion.gif" ) );
            inspectorPanel.activate( assertionInspector );
            break;
         }
         case UNKNOWN:
         {
            assertionInspector.setIcon( UISupport.createImageIcon( "/unknown_assertion.gif" ) );
            break;
         }
         case VALID:
         {
            assertionInspector.setIcon( UISupport.createImageIcon( "/valid_assertion.gif" ) );
            inspectorPanel.deactivate();
            break;
         }
      }
   }

   private JComponent buildLogPanel()
   {
      logArea = new JTextArea();
      logArea.setEditable( false );
      logArea.setToolTipText( "Response Log" );

      JPanel panel = new JPanel( new BorderLayout() );
      panel.add( new JScrollPane( logArea ), BorderLayout.CENTER );

      return panel;
   }

   public void setContent( JComponent content )
   {
      inspectorPanel.setContentComponent( content );
   }

   public void removeContent( JComponent content )
   {
      inspectorPanel.setContentComponent( null );
   }

   @Override
   protected void createToolbar( JXToolBar toolbar )
   {
      toolbar.addUnrelatedGap();
      toolbar.addFixed( new JLabel( "Path" ) );
      toolbar.addRelatedGap();
      pathField = new JTextField( getModelItem().getPath(), 15 );
      pathField.getDocument().addDocumentListener( new DocumentListenerAdapter()
      {

         @Override
         public void update( Document document )
         {
            getModelItem().setPath( pathField.getText() );
         }
      } );

      toolbar.addFixed( pathField );

      toolbar.addUnrelatedGap();
      toolbar.addFixed( new JLabel( "Port" ) );
      toolbar.addRelatedGap();
      portField = new JTextField( String.valueOf( getModelItem().getPort() ), 5 );
      portField.getDocument().addDocumentListener( new DocumentListenerAdapter()
      {

         @Override
         public void update( Document document )
         {
            try
            {
               getModelItem().setPort( Integer.parseInt( portField.getText() ) );
            }
            catch( NumberFormatException e )
            {
            }
         }
      } );

      toolbar.addFixed( portField );
   }

   private JComponent buildQueryMatchPanel()
   {
      JPanel panel = new JPanel( new BorderLayout() );
      panel.add( buildQueryMatchToolbar(), BorderLayout.NORTH );
      JSplitPane splitPane = UISupport.createHorizontalSplit( buildQueryEditor(), buildMatchEditor() );
      panel.add( splitPane, BorderLayout.CENTER );
      splitPane.setDividerLocation( 0.5f );
      return panel;
   }

   private Component buildMatchEditor()
   {
      JPanel panel = new JPanel( new BorderLayout() );

      matchEditorModel = new ModelItemPropertyEditorModel<WsdlMockResponseTestStep>( getModelItem(), "match" );
      panel.add( UISupport.getEditorFactory().buildXmlEditor( matchEditorModel ), BorderLayout.CENTER );

      UISupport.addTitledBorder( panel, "Matching Value" );

      return panel;
   }

   private Component buildQueryEditor()
   {
      JPanel panel = new JPanel( new BorderLayout() );

      queryEditorModel = new ModelItemPropertyEditorModel<WsdlMockResponseTestStep>( getModelItem(), "query" );
      panel.add( UISupport.getEditorFactory().buildXPathEditor( queryEditorModel ), BorderLayout.CENTER );

      UISupport.addTitledBorder( panel, "XPath Query" );

      return panel;
   }

   private Component buildQueryMatchToolbar()
   {
      JXToolBar toolBar = UISupport.createSmallToolbar();
      return toolBar;
   }

   private AssertionsPanel buildAssertionsPanel()
   {
      assertionsPanel = new AssertionsPanel( getModelItem() )
      {
         protected void selectError( AssertionError error )
         {
            ModelItemXmlEditor<?, ?> editor = getResponseEditor();
            editor.requestFocus();
         }
      };

      return assertionsPanel;
   }

   @Override
   public boolean onClose( boolean canCancel )
   {
      getModelItem().getTestCase().removeTestRunListener( testRunListener );
      SoapUI.getTestMonitor().removeTestMonitorListener( testMonitorListener );
      assertionsPanel.release();

      queryEditorModel.release();
      matchEditorModel.release();

      inspectorPanel.release();

      return super.onClose( canCancel );
   }

   public void setEnabled( boolean enabled )
   {
      super.setEnabled( enabled );

      pathField.setEnabled( enabled );
      portField.setEnabled( enabled );
   }

   public boolean dependsOn( ModelItem modelItem )
   {
      return modelItem == getModelItem() || modelItem == getModelItem().getTestCase()
              || modelItem == getModelItem().getOperation() || modelItem == getModelItem().getOperation().getInterface()
              || modelItem == getModelItem().getTestCase().getTestSuite()
              || modelItem == getModelItem().getTestCase().getTestSuite().getProject();
   }

   public class InternalTestRunListener extends TestRunListenerAdapter
   {
      @Override
      public void afterRun( TestRunner testRunner, TestRunContext runContext )
      {
         setEnabled( true );
      }

      @Override
      public void beforeRun( TestRunner testRunner, TestRunContext runContext )
      {
         setEnabled( false );
      }

      @Override
      public void beforeStep( TestRunner testRunner, TestRunContext runContext )
      {
         if( runContext.getCurrentStep() == getModelItem() )
         {
            logArea.setText( logArea.getText() + new Date( System.currentTimeMillis() ).toString() +
                    ": Waiting for request on http://127.0.0.1:" + getModelItem().getPort() + getModelItem().getPath() + "\r\n" );
         }
      }

      @Override
      public void afterStep( TestRunner testRunner, TestRunContext runContext, TestStepResult result )
      {
         if( result.getTestStep() == getModelItem() )
         {
            String msg = new Date( result.getTimeStamp() ).toString() + ": Handled request in " + result.getTimeTaken() + "ms";
            logArea.setText( logArea.getText() + msg + "\r\n" );
         }
      }
   }

   private class InternalTestMonitorListener extends TestMonitorListenerAdapter
   {
      public void loadTestFinished( LoadTestRunner runner )
      {
         setEnabled( !SoapUI.getTestMonitor().hasRunningTest( getModelItem().getTestCase() ) );
      }

      public void loadTestStarted( LoadTestRunner runner )
      {
         if( runner.getLoadTest().getTestCase() == getModelItem().getTestCase() )
            setEnabled( false );
      }

      public void testCaseFinished( TestRunner runner )
      {
         setEnabled( !SoapUI.getTestMonitor().hasRunningTest( getModelItem().getTestCase() ) );
      }

      public void testCaseStarted( TestRunner runner )
      {
         if( runner.getTestCase() == getModelItem().getTestCase() )
            setEnabled( false );
      }
   }

   public void propertyChange( PropertyChangeEvent evt )
   {
      super.propertyChange( evt );

      if( evt.getPropertyName().equals( WsdlMockResponseTestStep.STATUS_PROPERTY ) )
         updateStatusIcon();
   }

   private final class DeclareNamespacesAction extends AbstractAction
   {
      public DeclareNamespacesAction()
      {
         putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/declareNs.gif" ) );
         putValue( Action.SHORT_DESCRIPTION, "Declare available response/request namespaces in source/target expressions" );
      }

      public void actionPerformed( ActionEvent e )
      {
         try
         {
            WsdlMockResult lastResult = getMockResponse().getMockResult();
            String content = null;
            if( lastResult == null )
            {
               if( !UISupport.confirm( "Missing last result, declare from default request instead?", "Declare Namespaces" ) )
               {
                  return;
               }

               content = getMockResponse().getMockOperation().getOperation().createRequest( true );
            }
            else
            {
               content = lastResult.getMockRequest().getRequestContent();
            }


            String path = getModelItem().getQuery();
            if( path == null )
               path = "";

            getModelItem().setQuery( XmlUtils.declareXPathNamespaces( content ) + path );
         }
         catch( Exception e1 )
         {
            UISupport.showErrorMessage( e1 );
         }
      }
   }

}

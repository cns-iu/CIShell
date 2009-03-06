/* 
 * InfoVis CyberInfrastructure: A Data-Code-Compute Resource for Research
 * and Education in Information Visualization (http://iv.slis.indiana.edu/).
 * 
 * Created on Jan 24, 2005 at Indiana University.
 */
package org.cishell.reference.gui.persistence.load;

import java.io.File;
import java.util.ArrayList;

import org.cishell.framework.CIShellContext;
import org.cishell.framework.algorithm.AlgorithmFactory;
import org.cishell.framework.data.BasicData;
import org.cishell.framework.data.Data;
import org.cishell.reference.gui.common.AbstractDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 *
 * @author Team IVC (Weixia Huang, James Ellis)
 */
public class SelectedFileServiceSelector extends AbstractDialog {
	private File selectedFile;
	private LogService logger;

	private ServiceReference[] persisterArray;
	private List persisterList;
	private StyledText detailPane;
	private CIShellContext ciShellContext;
	private BundleContext bundleContext;
	private ArrayList returnList;

	private static final String[] DETAILS_ITEM_KEY = 
	{"format_name", "supported_file_extension", "format_description" };

	/*
	 * Other possible keys could be restorable_model_name, restorable_model_description
	 * */
	
	private static final String[] DETAILS_ITEM_KEY_DISPLAY_VALUE = 
	{"Format name", "Supported file extension", "Format description"};
	
	/*
	 * Other possible keys display values could be "Restorable model name", "Restorable model description"
	 * */

	public SelectedFileServiceSelector(String title, File theFile, 
			Shell parent, CIShellContext ciContext, BundleContext bContext, 
			ServiceReference[] persisterArray, ArrayList returnList){
		super(parent, title, AbstractDialog.QUESTION);
		this.ciShellContext = ciContext;
		this.bundleContext = bContext;
		this.persisterArray = persisterArray;
		this.returnList = returnList;

		this.selectedFile = theFile;

		this.logger = (LogService) ciContext.getService(LogService.class.getName());
		//shall this part be moved out of the code?
		setDescription("The file you have selected can be loaded"
				+ " using the following formats.\n"
				+ "Please select one of them.");
		setDetails("This dialog allows the user to choose among all available " +
				"formats for loading the selected data model.  Choose any of the formats " +
		"to continue loading the dataset.");
	}    

	private Composite initGUI(Composite parent) {        
		Composite content = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;        
		content.setLayout(layout);        

		Group persisterGroup = new Group(content, SWT.NONE);
		//shall this label be moved out of the code?
		persisterGroup.setText("Loaded by");
		persisterGroup.setLayout(new FillLayout());        
		GridData persisterListGridData = new GridData(GridData.FILL_BOTH);
		persisterListGridData.widthHint = 200;
		persisterGroup.setLayoutData(persisterListGridData);

		persisterList = new List(persisterGroup, SWT.H_SCROLL |SWT.V_SCROLL | SWT.SINGLE);
		//        initPersisterArray();
		initPersisterList();
		persisterList.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				List list = (List)e.getSource();
				int selection = list.getSelectionIndex();
				if(selection != -1){                    
					updateDetailPane(persisterArray[selection]);
				}                
			}                      
		});

		Group detailsGroup = new Group(content, SWT.NONE);
		// shall this label be moved out of the code?    
				detailsGroup.setText("Details");       
				detailsGroup.setLayout(new FillLayout());        
				GridData detailsGridData = new GridData(GridData.FILL_BOTH);
				detailsGridData.widthHint = 200;
				detailsGroup.setLayoutData(detailsGridData);

				detailPane = initDetailPane(detailsGroup);

				persisterList.setSelection(0);
				updateDetailPane(persisterArray[0]);

				return content;
	}

	private void initPersisterList(){        
		for (int i = 0; i < persisterArray.length; ++i) {

			String name = (String)persisterArray[i].getProperty("converter_name");

			// if someone was sloppy enough to not provide a name, then use the
			// name of the class instead.
			if (name == null || name.length() == 0)
				name = persisterArray[i].getClass().getName();
			persisterList.add(name);
		}
	}


	private StyledText initDetailPane(Group detailsGroup) {
		StyledText detailPane = new StyledText(detailsGroup, SWT.H_SCROLL | SWT.V_SCROLL);
		detailPane.setEditable(false);
		detailPane.getCaret().setVisible(false);
		return detailPane;
	}

	private void updateDetailPane(ServiceReference persister) {

		detailPane.setText("");
		for (int i=0; i<DETAILS_ITEM_KEY.length; i++){
			String val = (String) persister.getProperty(DETAILS_ITEM_KEY[i]);

			StyleRange styleRange = new StyleRange();
			styleRange.start = detailPane.getText().length();
			detailPane.append(DETAILS_ITEM_KEY_DISPLAY_VALUE[i] + ":\n");                            	
			styleRange.length = DETAILS_ITEM_KEY[i].length() + 1;
			styleRange.fontStyle = SWT.BOLD;
			detailPane.setStyleRange(styleRange);

			detailPane.append(val + "\n");               

		}

	}    

	private void selectionMade(int selectedIndex) {
		AlgorithmFactory persister =(AlgorithmFactory) bundleContext.getService(persisterArray[selectedIndex]);
		Data[] dataManager = null;
		boolean loadSuccess = false;

		try {
			dataManager = new Data[]{new BasicData(selectedFile.getPath(),String.class.getName())};
			dataManager = persister.createAlgorithm(dataManager, null, ciShellContext).execute();
			loadSuccess = true;
		} catch (Throwable e) {
			this.logger.log(LogService.LOG_ERROR, "Error occurred while executing selection", e);
			e.printStackTrace();
			loadSuccess = false;
		}

		if (dataManager != null && loadSuccess) {
			logger.log(LogService.LOG_INFO, "Loaded: "+selectedFile.getPath());

			for(int i = 0; i<dataManager.length; i++){
				returnList.add(dataManager[i]);
			}
			close(true); 
		} else {
			logger.log(LogService.LOG_ERROR, "Unable to load with selected loader");
		}
	}

	public void createDialogButtons(Composite parent) {
		Button select = new Button(parent, SWT.PUSH);
		select.setText("Select");
		select.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				int index = persisterList.getSelectionIndex();
				if(index != -1){
					selectionMade(index);
				}
			}
		});

		Button cancel = new Button(parent, SWT.NONE);
		cancel.setText("Cancel");
		cancel.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				close(false);
			}
		});
	}

	public Composite createContent(Composite parent) {
		return initGUI(parent);
	}
}

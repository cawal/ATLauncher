package ualberta.edu.guana.tools.atl;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.BasicExtendedMetaData;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.m2m.atl.emftvm.EmftvmFactory;
import org.eclipse.m2m.atl.emftvm.ExecEnv;
import org.eclipse.m2m.atl.emftvm.Metamodel;
import org.eclipse.m2m.atl.emftvm.Model;
import org.eclipse.m2m.atl.emftvm.impl.resource.EMFTVMResourceFactoryImpl;
import org.eclipse.m2m.atl.emftvm.util.DefaultModuleResolver;
import org.eclipse.m2m.atl.emftvm.util.ModuleResolver;
import org.eclipse.m2m.atl.emftvm.util.TimingData;

import picocli.CommandLine;
import picocli.CommandLine.Parameters;

/**
 * An off-the-shelf launcher for ATL/EMFTVM transformations
 * @author Victor Guana - guana@ualberta.ca
 * University of Alberta - SSRG Lab.
 * Edmonton, Alberta. Canada
 * Using code examples from: https://wiki.eclipse.org/ATL/EMFTVM
 */
public class ATLLauncher implements Callable<Integer> {
	
	// Some constants for quick initialization and testing.
	
	@Parameters(index = "0",
			description = "The input metamodel name in ATL",
			paramLabel = "input_metamodel_name")
	public String IN_METAMODEL_NAME; // = "Composed";
	
	@Parameters(index = "1",
			description = "The input metamodel path",
			paramLabel = "input_metamodel")
	public String IN_METAMODEL;// = "./metamodels/Composed.ecore";
	
	@Parameters(index = "2",
			description = "The input model path",
			paramLabel = "input_model")
	public String IN_MODEL;// = "./models/composed.xmi";
	
	@Parameters(index = "3",
			description = "The output metamodel name in ATL",
			paramLabel = "output_metamodel_name")
	public String OUT_METAMODEL_NAME;// = "Simple";

	@Parameters(index = "4",
			description = "The output metamodel path",
			paramLabel = "ouput_metamodel")
	public String OUT_METAMODEL;// = "./metamodels/Simple.ecore";
	
	@Parameters(index = "5",
			description = "The output model path",
			paramLabel = "ouput_model")
	public String OUT_MODEL;// = "./models/simple.xmi";
	
	@Parameters(index = "6",
			description = "The directory to find the transfomation definition (ASM)",
			paramLabel = "transformation_dir")
	public String TRANSFORMATION_DIR;// = "./transformations/";
	
	@Parameters(index = "7",
			description = "The module name of the transformation",
			paramLabel = "transformation_module")
	public String TRANSFORMATION_MODULE;//= "Composed2Simple";
	
	// The input and output metamodel nsURIs are resolved using lazy registration of metamodels, see below.
	private String inputMetamodelNsURI;
	private String outputMetamodelNsURI;
	
	//Main transformation launch method
	public void launch(String inMetamodelPath, String inModelPath, String outMetamodelPath,
			String outModelPath, String transformationDir, String transformationModule){
		
		/* 
		 * Creates the execution environment where the transformation is going to be executed,
		 * you could use an execution pool if you want to run multiple transformations in parallel,
		 * but for the purpose of the example let's keep it simple.
		 */
		ExecEnv env = EmftvmFactory.eINSTANCE.createExecEnv();
		ResourceSet rs = new ResourceSetImpl();

		/*
		 * Load meta-models in the resource set we just created, the idea here is to make the meta-models
		 * available in the context of the execution environment, the ResourceSet is later passed to the
		 * ModuleResolver that is the actual class that will run the transformation.
		 * Notice that we use the nsUris to locate the metamodels in the package registry, we initialize them 
		 * from Ecore files that we registered lazily as shown below in e.g. registerInputMetamodel(...) 
		 */
		Metamodel inMetamodel = EmftvmFactory.eINSTANCE.createMetamodel();
		inMetamodel.setResource(rs.getResource(URI.createURI(inputMetamodelNsURI), true));
		env.registerMetaModel(IN_METAMODEL_NAME, inMetamodel);

		Metamodel outMetamodel = EmftvmFactory.eINSTANCE.createMetamodel();
		outMetamodel.setResource(rs.getResource(URI.createURI(outputMetamodelNsURI), true));
		env.registerMetaModel(OUT_METAMODEL_NAME, outMetamodel);

		/*
		 * Create and register resource factories to read/parse .xmi and .emftvm files,
		 * we need an .xmi parser because our in/output models are .xmi and our transformations are
		 * compiled using the ATL-EMFTV compiler that generates .emftvm files
		 */
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("emftvm", new EMFTVMResourceFactoryImpl());
		
		// Load models
		Model inModel = EmftvmFactory.eINSTANCE.createModel();
		inModel.setResource(rs.getResource(URI.createURI(inModelPath, true), true));
		env.registerInputModel("IN", inModel);
		
		Model outModel = EmftvmFactory.eINSTANCE.createModel();
		outModel.setResource(rs.createResource(URI.createURI(outModelPath)));
		env.registerOutputModel("OUT", outModel);
		
		/*
		 *  Load and run the transformation module
		 *  Point at the directory your transformations are stored, the ModuleResolver will 
		 *  look for the .emftvm file corresponding to the module you want to load and run
		 */
		ModuleResolver mr = new DefaultModuleResolver(transformationDir, rs);
		TimingData td = new TimingData();
		System.err.println(transformationDir);
		System.err.println(transformationModule);
		env.loadModule(mr, transformationModule);
		td.finishLoading();
		env.run(td);
		td.finish();
			
		// Save models
		try {
			outModel.getResource().save(Collections.emptyMap());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * I seriously hate relying on the eclipse facilities, and if you're not building an eclipse plugin
	 * you can't rely on eclipse's registry (let's say you're building a stand-alone tool that needs to run ATL
	 * transformation, you need to 'manually' register your metamodels) 
	 * This method does two things, it initializes an Ecore parser and then programmatically looks for
	 * the package definition on it, obtains the NsUri and registers it.
	 */
	private String lazyMetamodelRegistration(String metamodelPath){
		
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
   	
	    ResourceSet rs = new ResourceSetImpl();
	    // Enables extended meta-data, weird we have to do this but well...
	    final ExtendedMetaData extendedMetaData = new BasicExtendedMetaData(EPackage.Registry.INSTANCE);
	    rs.getLoadOptions().put(XMLResource.OPTION_EXTENDED_META_DATA, extendedMetaData);
	
	    Resource r = rs.getResource(URI.createFileURI(metamodelPath), true);
	    EObject eObject = r.getContents().get(0);
	    // A meta-model might have multiple packages we assume the main package is the first one listed
	    if (eObject instanceof EPackage) {
	        EPackage p = (EPackage)eObject;
	        System.out.println(p.getNsURI());
	        EPackage.Registry.INSTANCE.put(p.getNsURI(), p);
	        return p.getNsURI();
	    }
	    return null;
	}
	
	/*
	 * As shown above we need the inputMetamodelNsURI and the outputMetamodelNsURI to create the context of
	 * the transformation, so we simply use the return value of lazyMetamodelRegistration to store them.
	 * -- Notice that the lazyMetamodelRegistration(..) implementation may return null in case it doesn't 
	 * find a package in the given metamodel, so watch out for malformed metamodels.
	 * 
	 */
	public void registerInputMetamodel(String inputMetamodelPath){	
		inputMetamodelNsURI = lazyMetamodelRegistration(inputMetamodelPath);
	}

	public void registerOutputMetamodel(String outputMetamodelPath){
		outputMetamodelNsURI = lazyMetamodelRegistration(outputMetamodelPath);
	}
	
	/*
	 *  A main method.
	 */	
	public static void main(String ... args){
		int exitCode = new CommandLine(new ATLLauncher()).execute(args);
		System.exit(exitCode);	
	}
	
	@Override
	public Integer call() throws Exception {
		

		System.out.println(TRANSFORMATION_DIR);
		System.out.println(TRANSFORMATION_MODULE);
		
		//ATLLauncher l = new ATLLauncher();
		this.registerInputMetamodel(IN_METAMODEL);
		this.registerOutputMetamodel(OUT_METAMODEL);
		this.launch(IN_METAMODEL, IN_MODEL, OUT_METAMODEL, OUT_MODEL, TRANSFORMATION_DIR, TRANSFORMATION_MODULE);
	
		return 0;
		
	}
}

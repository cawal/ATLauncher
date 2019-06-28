#!/bin/bash 

#Missing required parameters: input_metamodel_name, input_metamodel, input_model, output_metamodel_name, output_metamodel, output_model, transformation_dir, transformation_module
#Usage: <main class> input_metamodel_name input_metamodel input_model
#                    output_metamodel_name output_metamodel output_model
#                    transformation_dir transformation_module
#      input_metamodel_name   The input metamodel name in ATL
#      input_metamodel        The input metamodel path
#      input_model            The input model path
#      output_metamodel_name  The output metamodel name in ATL
#      output_metamodel        The output metamodel path
#      output_model            The output model path
#      transformation_dir     The directory to find the transfomation definition
#                               (ASM)
#      transformation_module  The module name of the transformation
#

echo $1

echo $2

input_metamodel_name="aadl" 
input_metamodel="/home/cawal/git/lssb/Activity-REST/activity-rest-framework/dsl/bundles/br.usp.ffclrp.dcm.lssb.restaurant.analysisactivitydescription/model/analysis-activity-description.ecore"      
input_model="${1}"          
output_metamodel_name="openapi"
output_metamodel="/home/cawal/git/third_party/openapi-metamodel/plugins/edu.uoc.som.openapi/model/openapi.ecore"      
output_model="${2}"          
transformation_dir="/home/cawal/git/lssb/Activity-REST/activity-rest-framework/transformations/aadlXmiToJsonOpenApi/src/main/java/br/usp/ffclrp/dcm/lssb/activityrest/"   
transformation_module="aadl2openAPI"


echo java -jar target/atlauncher-1.0.0-SNAPSHOT-jar-with-dependencies.jar ${input_metamodel_name} ${input_metamodel} ${input_model} ${output_metamodel_name} ${output_metamodel} ${output_model} ${transformation_dir} ${transformation_module}

java -jar target/atlauncher-1.0.0-SNAPSHOT-jar-with-dependencies.jar ${input_metamodel_name} ${input_metamodel} ${input_model} ${output_metamodel_name} ${output_metamodel} ${output_model} ${transformation_dir} ${transformation_module}

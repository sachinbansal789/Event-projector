ORG=uk
echo "Organisation Name: " $ORG

# todo - handle multiple environments either an array of environm,ents and a loop so we can build different
# config files for different environments
echo -n "Select the environment for generating base64 edge_config (dev/int/prod-internal)? "
read answer
ENV=$answer

echo "Environment: " $ENV
#
# convert the configuration file
# to a 64bit encoded file or standard out
#
# ************** NB **************************
# To run this as part of a jenkins job
# the program base64 will need to be available
#
# get the input file & path as a parameter
#
#
#  create the 64bit encoded output to file
#

INPUT_FILE=config/$ORG-$ENV-config.yaml
OUTPUT_FILE=config/$ORG-$ENV-B64-config.yaml

base64 --wrap=0 $INPUT_FILE > $OUTPUT_FILE
RC=$?
if [ $RC -gt 0 ];then
      echo "failed to create base 64 encoded config yaml file"
      exit $RC
fi
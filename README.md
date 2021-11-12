# ESW Observing Simulation

## To run the complete setup follow below steps:

Run the shell scripts in this order in different terminals.

1. sh simulation/start-csw-services.sh
2. sh simulation/start-esw-services.sh
3. sh simulation/start-components.sh
4. sh simulation/start-eng-ui.sh

Once, everything is up and running, You can login to Eng UI app at this [Browser Link](http://localhost:8000/esw-ocs-eng-ui) with user `osw-user1`, Then you can Provision , configure for example 'IFS_Only' ObsMode and load `sequence.json` from `sample-configs` folder in this project.
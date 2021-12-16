# ESW Observing Simulation

This repo is a part of Simulated Observing package.

Simulation for an observation requires sequencer's script written for that particular observation.
These scripts required for this observation can be found [here](https://github.com/tmtsoftware/sequencer-scripts/tree/esw-observing-simulation).
The simulation also depends on `TCS` assemblies which are part of separate [repository](https://github.com/tmtsoftware/tcs-vslice).
Apart from the scripts & tcs assemblies, All the other components required for the simulation are part of this repository.

Repository has the following contents:
- Inside `iris` folder, code related to following assemblies are present.
   - iris.imager.filter
   - iris.imager.adc (dependency on tcs-assemblies)
   - iris.imager.detector
   - iris.ifs.detector
   - iris.ifs.scale
   - iris.ifs.res
- Inside `wfos` folder, code related to following assemblies are present.
   - wfos.red.filter 
   - wfos.blue.filter 
   - wfos.red.detector 
   - wfos.red.detector 
- `sample-configs` folder has sample configuration files which will be used while providing neccessary configs to start simulation
  also these files are for future reference. All these files in production would be present in svn of config service.
- `apps` & `simulation` folder's has pre-configured setup for starting a simulation. These folders need not require any modification for an observation.
- `integration` folder has automated integration tests which demonstrates, commands(Setup/Observe) flowing from top level component(esw-sequencer) to the lowest component(assembly).

## Run complete simulation

Run the shell scripts in this order in different terminals.

1. sh simulation/start-csw-services.sh 
    - this maintains which esw & sequencer-script sha to be used in the simulation observation.
    - it starts required csw services : location service, config-service & aas server. 
2. sh simulation/start-esw-services.sh
    - this maintains which esw & sequencer-script sha to be used in the simulation observation. 
    - it starts required esw services for observation : sequence manager, agent service along with that 6 agents(Machines) are also started.
3. sh simulation/start-components.sh
    - this scripts starts the container containing all the `WFOS` & `IRIS` assemblies using `esw-agent-akka-app`.
    - this script uses `HostConfig.conf` which has entries of the `IRIS` & `WFOS` container.
4. To start tcs assemblies, refer the following [document](https://github.com/tmtsoftware/tcs-vslice-0.4#running-the-pk-assembly).
>   ⚠️ `HostConfig.conf` file has a placeholder field `configFilePath` which gets updated according to the **user's working directory** on the very first run of `start-components.sh`, so Do not check in the locally changed `sample-configs/HostConfig.conf`.

5. sh simulation/start-eng-ui.sh
    - It builds `esw-ocs-eng-ui` app using the latest code from esw-ocs-eng-ui repo & starts serving it on `http:localhost:8000/esw-ocs-eng-ui/` to manage observation from browser.

Once, everything is up and running, You can login to Eng UI app at this [Browser Link](http://localhost:8000/esw-ocs-eng-ui) with user `osw-user1`.

- First, we need to provision sequence components, which can be done on `Manage Infrastructure page`. Provision let's say 3 sequence components for `IRIS`, `TCS` & `ESW` subsystem.
- Once provisioned, Configure an obsmode either from Manage Observation / Manage Infrastructure page. For e.g. IRIS_ImagerAndIFS obsMode.
- Once configuration is successful, We need to submit sequence to Top level sequencer (ESW.IRIS_ImagerAndIFS).
- Click on `ESW.IRIS_ImagerAndIFS` box on Manage Infrastructure page to go to Observation Detail page of `ESW.IRIS_ImagerAndIFS`.
- On this page `sample-configs/esw_imager_and_ifs_sequence.json` sequence can be loaded using `Load Sequence` submit.
- Once loaded, User can Start sequence from the left panel on Observation Detail page, and visualise how each steps getting executed.
 
## How to run automated test

The following command will execute automated tests in their respective folders.
> cd ~/esw-observation-simulation/integration
> 
> sbt test
---
> cd ~/esw-observation-simulation/iris
> 
> sbt test
---
> cd ~/esw-observation-simulation/wfos
> 
> sbt test



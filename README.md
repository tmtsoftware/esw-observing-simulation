# ESW Observing Simulation

This repo is a part of Simulated Observing package.

Simulation for an observation requires sequencer's script written for that particular observation. These scripts
required for this observation can be
found [here](https://github.com/tmtsoftware/sequencer-scripts/tree/esw-observing-simulation). The simulation also
depends on `TCS` assemblies which are part of separate [repository](https://github.com/tmtsoftware/tcs-vslice-0.4).
Apart from the scripts & tcs assemblies, All the other components required for the simulation are part of this
repository.

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
- `sample-configs` folder has sample configuration files which will be used while providing necessary configs to start
  simulation also these files are for future reference. All these files in production would be present in svn of config
  service.
- `apps` & `simulation-scripts` folder's has pre-configured setup and scripts for starting a simulation. These folders
  need not require any modification for an observation.

## Run complete simulation

Run the shell scripts in this order in different terminals.

1. sh simulation-scripts/start-csw-services.sh
    - this maintains which esw & sequencer-script sha to be used in the simulation observation.
    - it starts required csw services : location service, config-service & aas server.
2. sh simulation-scripts/start-esw-services.sh
    - this maintains which esw & sequencer-script sha to be used in the simulation observation.
    - it starts required esw services for observation : sequence manager, agent service along with that 6 agents(
      Machines) are also started.
3. sh simulation-scripts/start-components.sh
    - this scripts starts the container containing all the `WFOS` & `IRIS` assemblies using `esw-agent-akka-app`.
    - this script uses `HostConfig.conf` which has entries of the `IRIS` & `WFOS` container.
4. sh simulation-scripts/start-tcs-assemblies.sh
    - this download tcs assemblies release zip and start them. For mac users, refer the following for
      pre-requisites [document](https://github.com/tmtsoftware/tcs-vslice-0.4#macos-12-monterey-intel-homebrew-installation-of-shared-library-dependencies)
      .
5. sh simulation-scripts/start-eng-ui.sh
    - It builds `esw-ocs-eng-ui` app using the latest code from esw-ocs-eng-ui repo & starts serving it
      on `http:localhost:8000/esw-ocs-eng-ui/` to manage observation from browser.
6. sh simulation-scripts/start-monitor-ui.sh
    - It runs `monitor-ui` app using the source code from monitor-ui folder & starts serving it
      on `http:localhost:9000/` to show events from observing-simulation.

Once, everything is up and running, You can login to Eng UI app at
this [Browser Link](http://localhost:8000/esw-ocs-eng-ui/) with user `osw-user1`.

- First, we need to provision sequence components, which can be done on `Manage Infrastructure page`. Provision let's
  say 3 sequence components for `IRIS`, `TCS` & `ESW` subsystem.
- Once provisioned, Configure an obsmode either from Manage Observation / Manage Infrastructure page. For e.g.
  IRIS_ImagerAndIFS obsMode.
- Once configuration is successful, We need to submit sequence to Top level sequencer (ESW.IRIS_ImagerAndIFS).
- Click on `ESW.IRIS_ImagerAndIFS` box on Manage Infrastructure page to go to Observation Detail page
  of `ESW.IRIS_ImagerAndIFS`.
- On this page `sample-sequences/esw_imager_and_ifs_sequence.json` sequence can be loaded using `Load Sequence` submit.
- Once loaded, User can Start sequence from the left panel on Observation Detail page, and visualise how each steps
  getting executed.

### Integration project

`integration` project has automated integration tests which demonstrates, commands(Setup/Observe) flowing from top level
component(esw-sequencer) to the lowest component(assembly). Following tests inside integration project verifies that the
communication is properly happening between the following components.

- IrisSequencerTest : iris-assemblies <-> iris-sequencer.
- WfosSequencerTest : wfos-assemblies <-> wfof-sequencer.
- TcsSequencerTest : tcs-assemblies <-> tcs-sequencer
- EswIrisSequencerTest : esw-sequencer <-> iris-sequencer && tcs-sequencer
- EswWfosSequencerTest : esw-sequencer <-> wfos-sequencer.

> cd ~/esw-observation-simulation/integration
>
> sbt test
---

## Version compatibility

| esw-observing-simulation | esw        | csw        | esw-ts     |
|--------------------------|------------|------------|------------|
| v0.2.0-RC1               | v0.5.0-RC2 | v5.0.0-RC2 | v0.4.0-RC2 |
| v0.1.0                   | v0.4.0     | v4.0.1     | v0.3.0     |
| v0.1.0-RC1               | v0.4.0-RC1 | v4.0.1-RC1 | v0.3.0-RC1 |

## Public Release History

| Date       | Tag    | Source                                                                            | Docs                                                                             | Assets                                                                                           |
|------------|--------|-----------------------------------------------------------------------------------|----------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| 2022-02-14 | v0.1.0 | [esw-observing-simulation-0.1.0](https://github.com/tmtsoftware/esw-observing-simulation/tree/v0.1.0) | [esw-observing-simulation-0.1.0 docs](https://github.com/tmtsoftware/esw-observing-simulation/blob/v0.1.0/README.md) | [esw-observing-simulation-0.1.0 assets](https://github.com/tmtsoftware/esw-observing-simulation/releases/tag/v0.1.0) |

## Pre-Release History

| Date       | Tag        | Source                                                                                                        | Docs                                                                                                                     |     | Assets                                                                                                                       |
|------------|------------|---------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|-----|------------------------------------------------------------------------------------------------------------------------------|
| 2022-10-19 | v0.2.0-RC1 | [esw-observing-simulation-0.2.0-RC1](https://github.com/tmtsoftware/esw-observing-simulation/tree/v0.2.0-RC1) | [esw-observing-simulation-0.2.0-RC1 docs](https://github.com/tmtsoftware/esw-observing-simulation/blob/v0.2.0/README.md) |     | [esw-observing-simulation-0.2.0-RC1 assets](https://github.com/tmtsoftware/esw-observing-simulation/releases/tag/v0.2.0-RC1) |
| 2022-02-08 | v0.1.0-RC1 | [esw-observing-simulation-0.1.0-RC1](https://github.com/tmtsoftware/esw-observing-simulation/tree/v0.1.0-RC1) | [esw-observing-simulation-0.1.0-RC1 docs](https://github.com/tmtsoftware/esw-observing-simulation/blob/v0.1.0/README.md) |     | [esw-observing-simulation-0.1.0-RC1 assets](https://github.com/tmtsoftware/esw-observing-simulation/releases/tag/v0.1.0-RC1) |

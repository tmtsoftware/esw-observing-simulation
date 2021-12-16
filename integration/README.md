### Integration project

This project has automated integration tests which demonstrates, commands(Setup/Observe) flowing from top level component(esw-sequencer) to the lowest component(assembly).

It depends on deploy modules of `esw-observation-simulation/iris`, `esw-observation-simulation/wfos` projects & esw's testkit.

Following tests inside integration project verifies that the communication is properly happening between the following components.

- IrisSequencerTest : iris-assemblies <-> iris-sequencer.
- WfosSequencerTest : wfos-assemblies <-> wfof-sequencer.
- EswIrisSequencerTest : esw-sequencer <-> iris-sequencer.
- EswWfosSequencerTest : esw-sequencer <-> iris-sequencer.

> cd ~/esw-observation-simulation/integration
>
> sbt test
---
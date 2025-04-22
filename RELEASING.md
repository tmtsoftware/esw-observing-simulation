# Releasing

## Versioning Strategy

TMT work packages are released in incrementally. (M1 -> RC -> Final)
A milestone release is cut from master branch as we expect bug fixes / feedback before making the final release.
1. While making `Milestone(M*)` release, we follow these ideas:
- Update transitive dependencies.
- Important bug fixes that we want in the major release.
2. While making `RC-*` release, we follow :
- We cut a branch from master, any changes on master from here onwards will not be considered for this current release.
- Do not update any third party dependencies.
  If Secondary packages has some changes after M1 Release of CSW, we update them & use their latest tags.
  These secondary packages include `sbt-docs` & `rtm`.
- Documentation related updates are allowed on this RC branch, because these changes won't be breaking anything code-wise.
3. While making `Final` release, we follow:
- RC branch will be considered final & not the master branch.
- After getting the approval, `V*.*.*` tag will be created.

## Steps 
___Important: Multiple versions need to be hard coded in multiple subprojects, including sequencer-scripts in Scala code!___

1. Create a branch named `branch-<major>.<minor>.x` if not already exists from `main`. Example branch name `branch-0.1.x`.
   All subsequent release for this release cycle should be done on this branch. All further steps are to be done on this branch.
2. Update version in `Common.scala` for `wfos`, `iris` & `integration` project (build.sbt).
3. Update csw.version in build.properties of `wfos`, `iris` & `integration` project.
4. Update `Libs.scala` & `plugins.sbt` in each of those project with latest version if applicable.
5. Update release notes (`notes/<version>.markdown`) in `esw-obsering-simulation` repo.
6. Update bodyFile field of create release step in .github/workflows/release.yml with the above markdown file. 
7. Update `CSW_VERSION`, `ESW_VERSION`, `SEQ_SCRIPT_VERSION`, `ENG_UI_VERSION` and `TCS_VERSION` in `versions.sh` with required version.
8. Update version field with upcoming tag for `iris-irisdeploy` & `wfos-wfosdeploy` in `sample-configs/HostConfig.conf`.
9. Update latest RTM version in Github and jenkins workflow files(this step to be done only in milestone & RC release).
10. Commit and push the changes to `branch-<major>.<minor>.x` branch.
11. Make sure build is green for dev for this branch.
12. Set `PROD=true` environment variable and Run `release.sh $VERSION$` script by providing version number argument.
    For example, for version 0.2.1-RC, the command would be `PROD=true ./release.sh v0.2.1-RC`. (This triggers release workflow)
13. Update version of sequencer-scripts repo (branch esw-observing-simulation) in integration/src/test/scala/esw/observing/simulation/ScriptVersion.scala (important for integration tests)
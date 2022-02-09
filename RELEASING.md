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

1. Create a branch named `branch-<major>.<minor>.x` if not already exists from `main`. Example branch name `branch-0.1.x`.
   All subsequent release for this release cycle should be done on this branch. All further steps are to be done on this branch.
2. Update version in `Common.scala` for `wfos`, `iris` & `integration` project.
3. Update csw.version in build.properties of `wfos`, `iris` & `integration` project.
4. Update `Libs.scala` & `plugins.sbt` with latest version if applicable.
5. Update release notes (`notes/<version>.markdown`) in `esw-obsering-simulation` repo.
6. Update bodyFile field of create release step in release.yml with the above markdown file. 
7. Update `CSW_VERSION` in `start-csw-services.sh`.
8. Update `ESW_VERSION & SEQ_SCRIPT_VERSION version` in `start-esw-services.sh`.
9. Update `V_SLICE_ZIP` & `version` with latest vslice zip url in `install-tcs-assemblies.sh`.
10. Update `version` with latest vslice zip url in `start-tcs-assemblies.sh`.
11. Update `ESW_VERSION` in `start-components.sh`.
12. Uncomment `Release section` & Comment out dev section, also update link of eng-ui release in `install-eng-ui.sh`.
13. Update version field with upcoming tag for `iris-irisdeploy` & `wfos-wfosdeploy` in `sample-configs/HostConfig.conf`.
14. Update latest RTM version in Github and jenkins workflow files(this step to be done only in milestone & RC release).
15. Commit and push the changes to `branch-<major>.<minor>.x` branch.
16. Make sure build is green for dev for this branch.
17. Set `PROD=true` environment variable and Run `release.sh $VERSION$` script by providing version number argument (This triggers release workflow)
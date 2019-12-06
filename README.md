<h1 align="center">
  <br>
  <img src="https://i.loli.net/2019/12/05/5FkrxA2PGeBjaS8.png">
  <br>
  GSC Core
  <br>
</h1>

# Welcome to GSC

## About

GSC (Global Social Chain) is the new generation of social networking chain. This social chain is determined to use block chain technology to subvert centralized social networks such as Facebook, WeChat, etc., which plays a dual role as "manager" and "arbiter" simultaneously for the former social networking platforms.


## Prepare dependencies

* JDK 1.8 (JDK 1.9+ are not supported yet)
* With Linux , ensure that the machine has __Oracle JDK 8__ .

## How to build

* Build in [IntelliJ IDEA](https://www.jetbrains.com/idea/) (community version is enough):

  1. Start IntelliJ. Select `File` -> `Open`, then locate to the gsc-core folder which you have git cloned to your local drive. Then click `Open` button on the right bottom.Branch of master is recommended.
  2. Check on `Use auto-import` on the `Import Project from Gradle` dialog. Select JDK 1.8 in the `Gradle JVM` option. Then click `OK`.
  3. IntelliJ will open the project and start gradle syncing, which will take several minutes, depending on your network connection and your IntelliJ configuration
  4. After the syncing finished, select `Gradle` -> `Tasks` -> `build`, and then double click `build` option.
   

## How to run

* You should modify the config.conf
  1. Replace existing entry in genesis.block.witnesses with your address.
  2. Replace existing entry in seed.node ip.list with your ip list.

* In IntelliJ IDEA
  1. After the building finishes, locate `FullNode` in the project structure view panel, which is on the path `gsc-core/src/main/java/org/gsc/program/Start`.
  2. Select `Start`, right click on it, and select `Run 'Start.main()'`, then `Start` starts running.

* In the Terminal
  Un the config.conf localwitness add your private key.
```bash
./gradlew run -Pwitness
```
## Read the [Wiki](https://wiki.gsc.social/) for detail

## Links
* [Webiste](https://gscan.social/)
* [GSC Exploer](https://gscan.social/)
* [Github](https://github.com/gscsocial)
* [Telegram](https://t.me/gscofficial)
* [Twitter](https://twitter.com/gsc_socialchain)
* [Facebook](https://www.facebook.com/GSCCoin/)
* [Meidum](https://medium.com/@gsc_socialchain)
* [Reddit](https://www.reddit.com/user/GSCOfficial/)
* [Youtube](https://www.youtube.com/channel/UCWcQhl4N6_ggZFdHwTxuuIQ)
 
## How to Contribute, Report bugs, issues using GitHub issues.

If you have a reasonable understanding of blockchain technology and at least some notions of Java you can of course 
contribute by using GitHub issues and Pull Requests. We also appreciate other types of contributions such as 
documentation improvements or even correcting typos in the code if you spot any.

The standard procedure is well documented on GitHub, for detailed explanation, especially if it’s the first time you’re 
doing this, you can follow the procedure on the following links:
[Working with forks](https://help.github.com/articles/working-with-forks/) and 
[Pull Requests](https://help.github.com/articles/proposing-changes-to-your-work-with-pull-requests/).
Basically, you fork the GSC repository, create a branch that clearly indicates the problem you’re solving. Later, when 
you are happy with your work, you create a Pull Request so we can review and discuss your implementation.

If the problem needs debating or you have questions on how to implement a feature, we would prefer you open a GitHub 
[issue](https://github.com/gscsocial/gsc-core/issues). If you spotted a typo or a code formatting issue, just directly 
opening a Pull Request is fine. 

License
GSC-Core is under the GNU General Public License v3. See LICENSE.

<h1 align="center">
  <br>
  <img width=20% src="https://i.loli.net/2018/05/03/5aeb2ffe1bc95.png">
  <br>
</h1>

## What's GSC

GSC(global social chain) is a project dedicated to building the infrastructure for a truly decentralized Internet.

The GSC Protocol, one of the largest blockchain based operating systems in the world, offers scalable, high-availability and high-throughput support that underlies all the decentralized applications in the GSC ecosystem. 

GSC enables large-scale development and engagement. With over 2000 transactions per second (TPS), high concurrency, low latency and massive data transmission, GSC is ideal for building decentralized entertainment applications. Free features and incentive systems allow developers to create premium app experiences for users.

GSC Protocol and the GSC Virtual Machine (TVM) allow anyone to develop decentralized applications (DAPPs) for themselves or their communities with smart contracts thereby making decentralized crowdfunding and token issuance easier than ever.

# How to Build

## Prepare dependencies

* JDK 1.8 (JDK 1.9+ are not supported yet)
* On Linux Ubuntu system (e.g. Ubuntu 16.04.4 LTS), ensure that the machine has [__Oracle JDK 8__](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-get-on-ubuntu-16-04), instead of having __Open JDK 8__ in the system.

## Getting the code with git

* Use Git from the Terminal, see the [Setting up Git](https://help.github.com/articles/set-up-git/) and [Fork a Repo](https://help.github.com/articles/fork-a-repo/) articles.
* develop branch: the newest code 
* master branch: more stable than develop.
In the shell command, type:
```bash
git clone https://github.com/gscsocial/gsc-core.git
git checkout -t origin/master
```

* For Mac, you can also install **[GitHub for Mac](https://mac.github.com/)** then **[fork and clone our repository](https://guides.github.com/activities/forking/)**. 

* If you'd rather not use Git, [Download the ZIP](https://github.com/gscsocial/gsc-core/archive/master.zip)

## Building from source code

* Build in the Terminal

```bash
cd gsc-core
./gradlew build
```

* Build an executable JAR

```bash
./gradlew clean shadowJar
```

* Build in [IntelliJ IDEA](https://www.jetbrains.com/idea/) (community version is enough):

  1. Start IntelliJ. Select `File` -> `Open`, then locate to the gsc-core folder which you have git cloned to your local drive. Then click `Open` button on the right bottom.
  2. Check on `Use auto-import` on the `Import Project from Gradle` dialog. Select JDK 1.8 in the `Gradle JVM` option. Then click `OK`.
  3. IntelliJ will open the project and start gradle syncing, which will take several minutes, depending on your network connection and your IntelliJ configuration
  4. After the syncing finished, select `Gradle` -> `Tasks` -> `build`, and then double click `build` option.
   
# Running

## Running a Private Testnet

### How to run a full node

* You should modify the config.conf
  1. Replace existing entry in genesis.block.witnesses with your address.
  2. Replace existing entry in seed.node ip.list with your ip list.

* In the Terminal

```bash
./gradlew run
```

* Use the executable JAR

```bash
cd build/libs 
java -jar gsc-core.jar 
```

* In IntelliJ IDEA
  1. After the building finishes, locate `FullNode` in the project structure view panel, which is on the path `gsc-cire/src/main/java/org/gsc/program/Start`.
  2. Select `Start`, right click on it, and select `Run 'Start.main()'`, then `Start` starts running.

* In the Terminal
  Un the config.conf localwitness add your private key.
```bash
./gradlew run -Pwitness
```
 
 
## How to Contribute

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


Follow us on:


[Telegram](https://t.me/gscofficial)


[Facebook](https://www.facebook.com/GSCCoin/)


[Twitter](https://twitter.com/gsc_socialchain)

[Meidum](https://medium.com/@gsc_socialchain)

[Reddit](https://www.reddit.com/user/GSCOfficial/)

[Youtube](https://www.youtube.com/channel/UCWcQhl4N6_ggZFdHwTxuuIQ)

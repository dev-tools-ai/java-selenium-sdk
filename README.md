[![dev-tools.ai sdk logo](https://dev-tools.ai/img/logo.svg)](https://dev-tools.ai/)

[![JDK-11+](https://img.shields.io/badge/JDK-11%2B-blue)](https://adoptium.net)
[![Maven Central](https://img.shields.io/maven-central/v/ai.dev-tools/ai-devtools-selenium)](https://search.maven.org/artifact/ai.dev-tools/ai-devtools-selenium)
[![Apache 2.0](https://img.shields.io/badge/Apache-2.0-blue)](https://www.apache.org/licenses/LICENSE-2.0)
[![Discord](https://img.shields.io/discord/974528356253065236?&logo=discord)](https://discord.gg/2J9WEYdq5C)
[![Twitter](https://img.shields.io/twitter/follow/DevToolsAI)](https://twitter.com/DevToolsAI)

The [dev-tools.ai](https://dev-tools.ai) selenium SDK is a simple library that makes it easy to write robust cross-browser web tests backed by computer vision and artificial intelligence.

dev-tools.ai integrates seamelessly with your existing tests, and will act as backup if your selectors break/fail by attempting to visually (computer vision) identify elements.

The dev-tools.ai SDK is able to accomplish this by automatically ingesting your selenium elements (using both screenshots and element names) when you run your test cases with dev-tools.ai for the first time.

The SDK is accompanied by a [web-based editor](https://smartdriver.dev-tools.ai/) which makes building visual test cases easy; you can draw boxes around your elements instead of using fragile CSS or XPath selectors.

## Install

Add the following line(s) to the dependencies section in your

**pom.xml (Maven)**

```xml
<dependencies>
    <dependency>
        <groupId>ai.dev-tools</groupId>
        <artifactId>ai-devtools-selenium</artifactId>
        <version>LATEST</version>
    </dependency>
</dependencies>

````

**build.gradle (Gradle)**
```groovy
implementation 'ai.dev-tools:ai-devtools-selenium:+'
```

## Tutorial
We have a detailed step-by-step tutorial which will help you get set up with the SDK: https://github.com/testdotai/java-selenium-sdk-demo

## Resources
* [Register/Login to your dev-tools.ai account](https://smartdriver.dev-tools.ai/signup)
* [Docs](https://dev-tools.ai/docs/get-started)
* [Another Tutorial](https://dev-tools.ai/docs/category/tutorial---selenium)

[![dev-tools.ai sdk logo](https://testdotai.github.io/static-assets/shared/logo-sdk.png)](https://dev-tools.ai/sdk)

[![JDK-11+](https://img.shields.io/badge/JDK-11%2B-blue)](https://adoptium.net)
[![javadoc](https://javadoc.io/badge2/ai.test.sdk/test-ai-selenium/javadoc.svg)](https://javadoc.io/doc/ai.test.sdk/test-ai-selenium)
[![Maven Central](https://img.shields.io/maven-central/v/ai.test.sdk/test-ai-selenium)](https://search.maven.org/artifact/ai.test.sdk/test-ai-selenium)
[![Apache 2.0](https://img.shields.io/badge/Apache-2.0-blue)](https://www.apache.org/licenses/LICENSE-2.0)
[![Discord](https://img.shields.io/discord/853669216880295946?&logo=discord)](https://sdk.dev-tools.ai/discord)
[![Twitter](https://img.shields.io/twitter/follow/testdotai)](https://twitter.com/testdotai)

The dev-tools.ai selenium SDK is a simple library that makes it easy to write robust cross-browser web tests backed by computer vision and artificial intelligence.

dev-tools.ai integrates seamelessly with your existing tests, and will act as backup if your selectors break/fail by attempting to visually (computer vision) identify elements.

The dev-tools.ai SDK is able to accomplish this by automatically ingesting your selenium elements (using both screenshots and element names) when you run your test cases with dev-tools.ai for the first time. 

The SDK is accompanied by a [web-based editor](https://smartdriver.dev-tools.ai/) which makes building visual test cases easy; you can draw boxes around your elements instead of using fragile CSS or XPath selectors.

## Install

Add the following line(s) to the dependencies section in your

**pom.xml (Maven)**

```xml

<dependency>
    <groupId>ai.dev-tools.sdk</groupId>
    <artifactId>devtools-ai-selenium</artifactId>
    <version>0.1.0</version>
</dependency>
````

**build.gradle (Gradle)**
```groovy
implementation 'ai.devtools.sdk:devtools-ai-selenium:0.1.0'
```

## Tutorial
We have a detailed step-by-step tutorial which will help you get set up with the SDK: https://github.com/testdotai/java-selenium-sdk-demo

## Resources
* [Register/Login to your dev-tools.ai account](https://smartdriver.dev-tools.ai/signup)
* [Docs](https://dev-tools.ai/docs/get-started)
* [Another Tutorial](https://dev-tools.ai/docs/category/tutorial---selenium)
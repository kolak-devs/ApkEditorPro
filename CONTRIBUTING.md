# CONRIBUTING

This document describes the basic principles of development that we adhere to. If you want your pull request to be approved, you have to fulfill several requirements.

## Step 1. Conventions
The project uses the [official Java code style](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html) from Oracle, as well as the official Kotlin style from JetBrains
All new modules starting from `2.0.0` should be written in Kotlin. If the implementation of a certain functionality requires rewriting a significant part of the code in the Java module, it is also recommended to write them in Kotlin.

## Step 2. Assistance
- Make the smallest possible changes that provide the necessary functionality.
- Before publishing changes, make sure that the project is being built and working correctly.
- Create a separate commit for each new function.
- Describe the innovations very clearly, the more information you give, the more likely it is that this change will be accepted.
- Use ["git rebase"](https://www.atlassian.com/git/tutorials/rewriting-history/git-rebase) to combine several commits into a number that can be viewed in an adequate period of time.

## Step 3. Important notes for contributors
- Do not update dependencies unless absolutely necessary (such as bug fixes and critical vulnerabilities)
- Don't use legacy File IO (java.io.File), for example for caching, instead use [New IO](https://wikipedia.org/wiki/New_I/O) (java.nio.Files)
- To interact with the device's file system, use the [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)
- To create lists, use RecyclerView + Mike Penz [Fast Adapter](https://github.com/mikepenz/FastAdapter).

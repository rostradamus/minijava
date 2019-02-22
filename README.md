# MiniJava base project

This is the starter repository for the MiniJava project. It does NOT contain the sample solution for the Functions project but starts of from the Functions starter where applicable. 

The repository is configured with maven to retrieve all necessary dependencies, as well as to run automatically the parsing classes on build.

## How do I setup this project
Look at the instructions for the functions-starter project.

## How do I run only the tests of a particular package from IntelliJ?
Right-click the package/class you want to run and click on "Run 'Tests in package.name'"

## How do I run only the tests of a particular package from the terminal?
- Go to the root module folder (ir, backend, or frontend)
- Run `mvn -DfailIfNoTests=false "-Dtest=test/parser/*.java" test` (replace test/parser for whichever the package/name is)

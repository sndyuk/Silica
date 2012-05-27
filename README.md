# Getting started  
## 0. Checkout modules

    $ git clone git://github.com/sndyuk/Silica.git
    $ cd Silica
    
***
## 1. Settings
### a. Tests on the local machine
 Nothing to do.
 
***
### b. Tests on the remote machine
##### Edit 'src/test/resource/exapmle.properties'  
`server.addresses=localhost` => `server.addresses=yourdomain.com`  

##### Open the TCP port 8089 and 50002 for RMI server on 'yourdomain'


## 2. Tests

    $ mvn test -Dmaven.test.skip=false
 
***

## 3. Creating a jar file

    $ mvn clean compile test-compile package -Dmaven.test.skip=false
    
    

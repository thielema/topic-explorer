TopicExplorer
=============

###Installation steps

####Generate ssh key and inform github about your public key
Follow the steps at https://help.github.com/articles/generating-ssh-keys .
After that clone the repository by executing in (some new) eclipse workspace folder
```
git clone git@github.com:hinneburg/TopicExplorer.git
```
####Install mariadb 10.0.14 or later 
For Ubuntu see article https://downloads.mariadb.org/mariadb/repositories/ .
The installation routine lets you chose a root password for the  mariadb
####Change some mariadb (mysql) defaults 
  - allow `load local infile`
This may not be neccessary in Ubuntu. You need to find maria(mysql)-server config file, 
for Ubuntu this is at `/etc/mysql/my.cnf`. Insert `local-infile=1` into both sections 
under `[mysqld]` and `[mysql]`.
  - set `innodb_buffer_pool_size` to a large size like `8GB`
  - set `ft_min_word_len=1` in case of Japanese words to allow fulltext search of small words
  - set `group_concat_max_len=1000000` to a large value to allow constucting Japanese documents from a table containing all tokenized words of each document. The parameter value needs to be an upper bound of the constructed document size.
####Create user as Mysql-Root and set privileges
using some mysql client, 
e.g. on Ubuntu: `mysql -u root -p`.
```
grant all privileges on <Maerchen Datenbank>.* to <user>@localhost identified by <password>;
grant file on *.* to <user>@localhost ;
```
####Create database as `<user>`
with some mysql client, e.g. on Ubuntu: `mysql -u <user> -p`.
```
create database <Maerchen Datenbank> CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```
####Download the document collection of Grimms fairy tales
  - Fulltexts: http://users.informatik.uni-halle.de/~hinnebur/maerchen/grimms_maerchen_without_duplicates.sql
  - Tokens: http://users.informatik.uni-halle.de/~hinnebur/maerchen/grimms_maerchen_without_duplicates_TE.csv

using an IP of uni-halle.de, e.g. login to the vpn of Uni Halle.
####Load documents into database
using some mysql client
e.g. on Ubuntu 
```
mysql -u <user> -p -D <Maerchen Datenbank> < <Path to File>grimms_maerchen_without_duplicates.sql
```
This creates and fills two tables with the structures
```
CREATE TABLE orgTable_meta (
  DOCUMENT_ID int(11) NOT NULL,
  TITLE VARCHAR(255) NOT NULL
);

CREATE TABLE orgTable_text (
  DOCUMENT_ID int(11) NOT NULL,
  DOCUMENT_TEXT  mediumtext
) ENGINE=INNODB;
```

####Create as developer the following two paths
`<path to your git copy>TopicExplorer/core-common/local/main/resources/` and 
`<path to your git copy>TopicExplorer/webapp/local/main/resources/`
and put into both the following three files with their respective contents.

Create file `config.local.properties`
```
InCSVFile=<path to file>/grimms_maerchen_without_duplicates_TE.csv
newTopics=true
DocBrowserLimit=20
plugins=text,colortopic,hierarchicaltopic
autocompleteMinChars=3
malletNumTopics=<Number of Topics>
TopicBestItemLimit=30
```
file `database.local.properties`
``` 
DbLocation=localhost:3306
DbUser=<user>
DbPassword=<password>
DB=<Maerchen Datenbank>
```
and file `log4j.local.properties`
```
# Root logger option
log4j.rootLogger=info, file, stdout
 
# Direct log messages to a log file
log4j.appender.file=org.apache.log4j.RollingFileAppender
log4j.appender.file.File=logs/<log-file-name>.log
log4j.appender.file.MaxFileSize=128MB
log4j.appender.file.MaxBackupIndex=3
log4j.appender.file.layout=org.apache.log4j.PatternLayout
log4j.appender.file.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c: %m%n
 
# Direct log messages to stdout
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Target=System.out
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c: %m%n
```
In file `log4j.local.properties` you can replace `<log-file-name>` with your own name for the log file to be created by TopicExplorer preprocessing and webapp.
####Install R
See http://www.r-project.org . For Ubuntu see http://wiki.ubuntuusers.de/R .
```
sudo apt-get install r-base 
sudo apt-get install r-recommended 
```
For other systems, make sure that `Rscript` command is in your general search path.
####Install eclipse kepler (or Luna)
from http://eclipse.org/ and install plugins via Help -> Install new Software
Select `Kepler - http://download.eclipse.org/releases/kepler` and chose the packages:
   - JST Server Adapters
   - JST Server Adapters Extensions
   - JST Server UI
   - m2e Maven Integration for Eclipse
   - m2e-WTP JAX-RS...
   - m2e-WTP JPA...
   - m2e-WTP JSF...
   - m2e-WTP Maven Integration

Download Apache TomCat 6.x zip file from 
https://tomcat.apache.org/download-60.cgi 
and extract it to some path.
Create a new server Apache TomCat 6. and reference the chosen path
File -> New -> Others -> Server
####Specify server settings
Create file `~/.m2/settings.xml`.

```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
	<servers>
		<server>
			<id>snapshots</id>
			<username>db-devRead</username>
			<password></password>
			<filePermissions>444</filePermissions>
			<directoryPermissions>444</directoryPermissions>
		</server>
                <server>
                        <id>snapshots-write</id>
                        <username>db-devWrite</username>
                        <password></password>
                        <filePermissions>644</filePermissions>
                        <directoryPermissions>744</directoryPermissions>
                </server>
 	</servers>
 </settings>
```
Ask the project manager about the passwords of  `db-devRead` and `db-devWrite`.

####Import Projects into Eclipse: 
Import -> Maven -> Existing Maven Project -> browse : Navigate to TopicExplorer Folder. 
Further, disable workspace resolution in Eclipse maven plugin: right click project, Maven -> Disable Workspace Resolution. This is important to prevent Eclipse from acidentally using artefacts from projects that are open in your workspace instead of the artefacts specified in dependencies in the `pom.xml`.

####Build the project
Mouse right click on TopicExplorer -> Run as -> Maven Build (at first time input goals: clean install)

####Run preprocessing
Open a console and navigate to the workspace. Then go into the distribution module
```
cd TopicExplorer/distribution/target/distribution-1.X-SNAPSHOT-preprocessing/

```
Make sure your local property files are in the right place
```
ls resources/
```
should show at least `config.local.properties`, `database.local.properties` and `log4j.local.properties`. 
When everything is fine, start the preprocessing
```
./bin/run-preprocessing.sh
```
This should create tables in your database, which are used by the web-based user interface. 
####Start webapp
Mouse right click on webapp -> Run -> Run on Server . 
In case of errors, do refresh (F5) on TopicExplorer and mouse right click -> Maven -> Update Projects. 
WebApp should appear in Eclipse-Browser. It is not functional there. 
You may use the webapp in Firefox or Safari http://localhost:8080/webapp/index.html


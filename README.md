A Java library to convert a sequence of JMS messages into a reactive stream. Exposes methods to retrieve an RxJava Observable or Flowable from the message sequence.

Once we retrieve an Observable / Flowable, the JMS messages can be manipulated using standard reactive stream methods, eg. map, filter, flatMap, zip, window, scan, and so on.

For usage, please browse through the sample programs in the sample directory.

To test the sample programs, we recommend the following steps:
i. Install OpenMQ,the reference implementation of the JMS secification on your machine. You can download OpenMQ from: https://javaee.github.io/openmq/Downloads.html\
ii. Start OpenMQ broker\
iii. Start OpenMQ admin server and create a QueueConnectionFactory named 'QCF', a Physical Queue named 'myqueue', and a JNDI resource mapped to the physical queue named 'myQ'\
iii. Build the rxjms project you cloned from github, using maven. The command to run, from the rxjms directory is: mvn clean install\
iv. Add the following files to the CLASSPATH:\
    rxjms-0.0.1-SNAPSHOT.jar (created within the target folder after build)\
    rxjava-2.1.12.jar (downloadable from maven repository)\
    reactive-streams-1.0.2.jar (downloadable from maven repository)\
    slf4j-api-1.7.25.jar (downloadable from maven repository)\
    jms.jar (from mq/lib directory under OpenMQ installation folder)\
    imq.jar (from mq/lib directory under OpenMQ installation folder)\
    fscontext.jar (from mq/lib directory under OpenMQ installation folder)
v. Run the sample applications:\
   java com.techyugadi.reactive.rxjms.sample.SimpleApp\
   or\
   java com.techyugadi.reactive.rxjms.sample.SampleApp

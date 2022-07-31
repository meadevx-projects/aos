Team Members:
-MANEESH ABRAHAM (mea130130)
-RUCHIR KEMNAIK (rkk130330)
-KARAN LUNIYA (kal130230)

How to run the program:
-----------------------
1)First run killalljava.sh script to kill any servers from any previous execution using the ports:

{dc01:~/aos/aos_project2/src}sh killalljava.sh

2)Compile if required:

{dc01:~/aos/aos_project2/src}javac *.java

3)Execute the program simultaneously across multiple machines using the launcher.sh script. The launcher script requires the protocol as an argument as the program can be executed using TCP as well as SCTP:

{dc01:~/aos/aos_project2/src}sh launcher.sh sctp
OR
{dc01:~/aos/aos_project2/src}sh launcher.sh tcp

3)Execute Testing script:

{dc01:~/aos/aos_project2/src}java AOSRCTesting

------------------------------------------------------------------------

# PicketLink Federation: Integration Tests #
 
This project provides some unit and integration tests for PicketLink Federation.

For more informations see https://docs.jboss.org/author/display/PLINK/Integration+Tests.

## Build Information ##

Follow the documentation above.

## Running with Java Security Manager

For running Java Security Manager add following to `mvn` command:

	-Dsmproperty="-Djava.security.manager -Djava.security.policy==/path/to/policy.policy"

Or use some system property:

	export SECMAN_PROPERTY="-Djava.security.manager -Djava.security.policy==/path/to/policy.policy"

And add to `mvn` command:

	-Dsmproperty=${SECMAN_PROPERTY}

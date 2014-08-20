#!/bin/sh
rabbitmqctl delete_vhost mule-test
rabbitmqctl delete_user mule

rabbitmqctl add_vhost mule-test
rabbitmqctl add_user mule elum
rabbitmqctl set_permissions -p mule-test mule ".*" ".*" ".*"


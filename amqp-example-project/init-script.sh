
# Under the folder {rabbitmq-server-home}/sbin run this script
#
# /mule-test is the virtual host to create
# mule:elum is the user and password created for the virtual host
#
# this data must be used in the (Global Element) AMQP Connector

sudo ./rabbitmqctl delete_vhost /mule-test
sudo ./rabbitmqctl delete_user mule

sudo ./rabbitmqctl add_vhost /mule-test
sudo ./rabbitmqctl add_user mule elum
sudo ./rabbitmqctl set_permissions -p /mule-test mule ".*" ".*" ".*"

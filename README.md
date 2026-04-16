# Distributed-Memory-IDS

This is a distributed memory model, assuming bidirectional communication, failure transparency 
and reliable network on a clique topology between compute nodes. We use a replicated memory approach, with Ricart and Agrawala’s mutual exlusion algorithm on a clique topology.

### Compile and run guidelines
Run `make client` if you want to run the client or `make controller` to instantiate a new node.
Before doing so, make sure there is a RabbitMQ broker available either in a distance or locally (assuming installed and configured) by running `rabbitmq-server`.

Arnaud & Andrej 2026 :D
# reservations

# Initial Requirements:
- Create an application to sell and validate tickets to a concert hall. There is currently one concert hall. So let's stick to creating 
ticketing to only that one.

## users:
- There is admin user, power users and regular users (customers)
  - Admin user is created by a script. His role is only needed to create power users (venue administrators). Further he has identical privileges as power users,
just his account cannot be removed
  - All power users are allowed to edit repertoire and manage tickets
  - Regular users can only buy tickets choosing the event. They register into application with an email address and a password.
For now no confirmation e-mail is being sent.

## events:
- An event has a date, time and capacity. Only one type of tickets are sold.
- Power users can see the state of tickets sold for an event
- Customers can see the repertoire with available tickets

## tickets
- Currently tickets are just reservations, since there is no technical way yet to process with payments 
- Customers can check their tickets (tickets they have bought with state future/past) with details visible


## technical requirement:
- java with spring boot project, maven
- logs stored in Elastic
- business data stored in PostgreSql
- communication with frontend with REST
- JWT used as a way to authenticate
- JWT secret stored for now in config file
- whole solution deployed as docker-compose

# Perspective:
- Tickets are sold at the set price but the price can be different starting at the specific dates (to be set up by power users)

# Technical perspective:
- move tickets to Cassandra?



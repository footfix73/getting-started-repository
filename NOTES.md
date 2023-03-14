# Utilizar MariaDB en Quarkus con Hibernate y Panache

Quarkus es un framework Java supersónico construido con una mentalidad cloud-native en primer lugar. Es muy rápido en el arranque y tiene una huella de memoria mucho menor en comparación con Spring Boot. Quarkus está bien integrado con Hibernate y funciona a la perfección. Para simplificar la complejidad de Hibernate, se puede utilizar Panache para lograr resultados similares a los del repositorio de datos de Spring. En este ejemplo, vamos a ver cómo utilizar MariaDB en Quarkus con Hibernate y Panache.

Vamos a construir un servicio de usuarios muy simple que tiene funcionalidad CRUD.  Para ello, vamos a incorporar Hibernate y Panache para construir repositorios similares a Spring Data.

## Vanilla Hibernate es difícil de utilizar
Podríamos utilizar únicamente Hibernate. Por lo tanto, me di cuenta de que tener una capa de abstracción, similar a Spring Data, es mucho más adecuado. Por eso elegí Panache.

## Arquitectura

Quarkus tiene muchas extensiones útiles para diversos fines. Son como diferentes frameworks o librerías de Spring. También hay muchas opciones disponibles para trabajar con bases de datos. Los principales candidatos son:

* Quarkus port de Spring Data JPA
* Panache

Aunque hay una diferencia importante entre Panache y Spring Data. **Panache** soporta tanto **Active Record** como **Repository** Pattern. Por lo tanto, da más libertad de acción a los desarrolladores en función de sus necesidades.

Nos quedamos con el patrón de repositorio convencional, ya que es un patrón superior y menos complicado de trabajar. 

## Conexión de Quarkus con Hibernate y Panache
Necesitamos cablear algunas cosas con Hibernate y Panache, para usar MariaDB en Quarkus.  
  
Vamos a empezar por añadir las dependencias relacionadas. Como ya hemos añadido las bibliotecas de controladores de Hibernate y MariaDB, sólo tenemos que añadir la dependencia de Panache.

```yml
<dependencies>
  <dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.26</version>
  </dependency>
  <dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-orm-panache</artifactId>
  </dependency>
  <dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-orm</artifactId>
  </dependency>
  <dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-validator</artifactId>
  </dependency>
  <dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-mariadb</artifactId>
  </dependency>
</dependencies>
```
Para completar, he añadido las dependencias de Hibernate y MariaDB al fragmento anterior.

## Añadir application.properties

Debemos establecer los detalles de MariaDB (como credenciales, puerto y demás) en el archivo application.properties.

```yml
quarkus.datasource.jdbc.url=jdbc:mariadb://localhost:3306/developer
quarkus.datasource.jdbc.driver=org.mariadb.jdbc.Driver
quarkus.datasource.username=developer
quarkus.datasource.password=developer
quarkus.hibernate-orm.database.generation=update
```
Esto no difiere mucho de cualquier configuración de Spring Boot.

## Modificación de la entidad User
Como ya tenemos la entidad User, sólo necesitamos modificarla. 
Vamos a añadir algunas anotaciones. Para que podamos utilizarlo con Hibernate.

``` java
package  org.acme.entity;

import  javax.persistence.Column;
import  javax.persistence.Entity;
import  javax.persistence.GeneratedValue;
import  javax.persistence.GenerationType;
import  javax.persistence.Id;
import  javax.persistence.Table;
import  javax.validation.constraints.Max;
import  javax.validation.constraints.Min;
import  javax.validation.constraints.NotBlank;
import  javax.validation.constraints.Size;
import  lombok.Data;
import  lombok.RequiredArgsConstructor;
 
@Data
@RequiredArgsConstructor
@Entity
@Table(name =  "users")
public  class  User {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private  long id;

@Column(name="first_name", nullable =  false)
@NotBlank
@Size(max =  256)
private  String firstName;

@Column(name="last_name", nullable =  false)
@NotBlank
@Size(max =  256)
private  String lastName;

@Column(name="age", nullable =  false)
@Min(1)
@Max(200)
private  int age;
}
```

## Creación del repositorio Panache
El siguiente paso es crear el UserRepository. Para ello, extendemos PanacheRepository.

```java
package  org.acme.repository;

import  io.quarkus.hibernate.orm.panache.PanacheRepository;
import  javax.enterprise.context.ApplicationScoped;
import  org.acme.entity.User;

@ApplicationScoped
public  class  UserRepository  implements  PanacheRepository<User> {
}
```
Añadir **@ApplicationScoped** permite a Quarkus gestionar el bean. Esta es una anotación Javax.

Sorprendentemente la clase no tiene ningún método. Eso es porque PanacheRepository tiene algunos métodos base que son suficientes para la mayoría de las operaciones CRUD.

## Implementación del servicio User
La implementación de UserService es sencilla. La única parte remotamente desafiante podría ser cómo inyectar el UserRepository al servicio.

```java
package  org.acme.service; 

import  java.util.List;
import  org.acme.entity.User;
import  org.acme.exception.UserNotFoundException;

public  interface  UserService {
  User getUserById(long id) throws UserNotFoundException;
  List<User> getAllUsers();
  User updateUser(long id, User user) throws  UserNotFoundException;
  User  saveUser(User user);
  void  deleteUser(long id) throws  UserNotFoundException;
}
```

```java
package  org.acme.service.impl;

import  java.util.List;
import  javax.enterprise.context.ApplicationScoped;
import  javax.inject.Inject;
import  javax.transaction.Transactional;
import  org.acme.entity.User;
import  org.acme.exception.UserNotFoundException;
import  org.acme.repository.UserRepository;
import  org.acme.service.UserService;

@ApplicationScoped
public class DefaultUserService implements UserService {

private final UserRepository userRepository;

@Inject
public DefaultUserService(UserRepository userRepository) {
this.userRepository = userRepository;
} 

@Override
public User getUserById(long id) throws  UserNotFoundException {

return userRepository.findByIdOptional(id).orElseThrow(() ->  new  UserNotFoundException("Este usuario no existe"));
}

@Override
public List<User> getAllUsers() {
  return userRepository.listAll();
} 

@Transactional
@Override
public User updateUser(long id, User user) throws  UserNotFoundException {
  User existingUser = getUserById(id);
  existingUser.setFirstName(user.getFirstName());
  existingUser.setLastName(user.getLastName());
  existingUser.setAge(user.getAge());

  userRepository.persist(existingUser);

return existingUser;
}

@Transactional
@Override
public User saveUser(User user) {
  userRepository.persistAndFlush(user);
  return user;
}

@Transactional
@Override
public void deleteUser(long id) throws  UserNotFoundException {
  userRepository.delete(getUserById(id));
}
}
```
Como puedes ver hemos utilizado la anotación @Inject para inyectar UserRepository. La anotación se añade al constructor del servicio. Ten en cuenta que @Inject es una anotación Javax, no la confundas con la anotación Google Inject.

## Actualizar UserController
El último paso es actualizar el UserController y deshacerse del conjunto ficticio. Y reemplazarlo con la implementación real de la siguiente manera,

```java
package  org.acme.controller;

import  java.util.List;
import  javax.inject.Inject;
import  javax.validation.Valid;
import  javax.validation.constraints.Max;
import  javax.validation.constraints.Min;
import  javax.validation.constraints.NotBlank;
import  javax.ws.rs.Consumes;
import  javax.ws.rs.DELETE;
import  javax.ws.rs.GET;
import  javax.ws.rs.POST;
import  javax.ws.rs.PUT;
import  javax.ws.rs.Path;
import  javax.ws.rs.PathParam;
import  javax.ws.rs.Produces;
import  javax.ws.rs.core.MediaType;
import  javax.ws.rs.core.Response;
import  org.acme.entity.User;
import  org.acme.exception.UserNotFoundException;
import  org.acme.service.UserService;

@Path("/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {

private final UserService userService;

@Inject
public UserController(UserService userService) {
  this.userService = userService;
}

@GET
public List<User> getUsers() {
  return userService.getAllUsers();
}

@GET
@Path("/{id}")
public User getUser(@PathParam("id") int id) throws  UserNotFoundException {
  return userService.getUserById(id);
}

@POST
public User createUser(@Valid UserDto userDto) {
  return userService.saveUser(userDto.toUser());
}

@PUT
@Path("/{id}")
public User updateUser(@PathParam("id") int id, @Valid  UserDto userDto) throws  UserNotFoundException {
  return userService.updateUser(id, userDto.toUser());
}

@DELETE
@Path("/{id}")
public Response deleteUser(@PathParam("id") int id) throws UserNotFoundException {
  userService.deleteUser(id);
  return Response.status(Response.Status.NO_CONTENT).build();
}

public static class UserDto {

@NotBlank
private String firstName;

@NotBlank
private String lastName;

@Min(value=1, message="The value must be more than 0")
@Max(value=200, message = "The value must be less than 200")
private int age;

public String getFirstName() {
return firstName;
} 

public void setFirstName(String firstName) {
  this.firstName = firstName;
} 

public String getLastName() {
  return lastName;
}

public void setLastName(String lastName) {
  this.lastName = lastName;
} 

public int getAge() {
  return age;
} 

public void setAge(int age) {
  this.age = age;
}
  
public User toUser() {
  User user = new User();
  user.setFirstName(firstName);
  user.setLastName(lastName);
  user.setAge(age);
  return user;
}
}
}
```

## Ejecutar el proyecto

Si ejecutamos el **docker-compose.yml** para tener bbdd MariaDB si no lo tenemos en local

```yml
version: "3.3"

services:
  db:
    image: mysql
    restart: always
    environment:
      MYSQL_DATABASE: developer
      MYSQL_USER: developer
      MYSQL_PASSWORD: developer
      MYSQL_ROOT_PASSWORD: developer
  ports:
    - "3306:3306"
  expose:
    - "3306"
  volumes:
    - mydb-db:/var/lib/mysql
volumes:
  mydb-db:
```
Corremos el proyecto una vez tengamos la base de datos MariaDB
``` bash
./mvnw quarkus:dev
```
o
```bash
./mvnw quarkus:dev -Ddebug
```

Eso abre el puerto 5005 al que puedes conectarte desde tu IDE.

## Curls de ejemplo

```bash
# get list of users (secured, accessible to users with 'ADMIN' or 'USER' role)
$ curl --anyauth --user leo:1234 localhost:8080/v1/users/

# get a specific user (secured, accessible to users with 'ADMIN' or 'USER' role)
$ curl --anyauth --user leo:1234 localhost:8080/v1/users/2

# create a user (open)
$ curl --request POST 'localhost:8080/v1/users' --header 'Content-Type: application/json' \
--data-raw '{
	"firstName": "Tom",
	"lastName": "Cruise",
	"age": 57
}'

# edit a user (secured, accessible to users with 'ADMIN' role only)
$ curl --anyauth --user admin:admin --request PUT 'localhost:8080/v1/users/1' --header 'Content-Type: application/json' \
--data-raw '{
	"firstName": "Leonardo",
	"lastName": "DiCaprio",
	"age": 46
}'

# delete a user (secured, accessible to users with 'ADMIN' role only)
$ curl --anyauth --user admin:admin --request DELETE 'localhost:8080/v1/users/2'
```
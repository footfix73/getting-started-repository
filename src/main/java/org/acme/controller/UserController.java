package org.acme.controller;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.acme.entity.User;
import org.acme.exception.UserNotFoundException;
import org.acme.service.UserService;


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
    public User getUser(@PathParam("id") int id) throws UserNotFoundException {
        return userService.getUserById(id);
    }

    @POST
    public User createUser(@Valid UserDto userDto) {
        return userService.saveUser(userDto.toUser());
    }

    @PUT
    @Path("/{id}")
    public User updateUser(@PathParam("id") int id, @Valid UserDto userDto) throws UserNotFoundException {
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

        @Min(value = 1, message = "The value must be more than 0")
        @Max(value = 200, message = "The value must be less than 200")
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

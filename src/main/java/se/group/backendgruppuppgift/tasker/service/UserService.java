package se.group.backendgruppuppgift.tasker.service;

import org.springframework.stereotype.Service;
import se.group.backendgruppuppgift.tasker.model.Task;
import se.group.backendgruppuppgift.tasker.model.TaskStatus;
import se.group.backendgruppuppgift.tasker.model.Team;
import se.group.backendgruppuppgift.tasker.model.User;
import se.group.backendgruppuppgift.tasker.model.web.TaskWeb;
import se.group.backendgruppuppgift.tasker.model.web.UserWeb;
import se.group.backendgruppuppgift.tasker.repository.TaskRepository;
import se.group.backendgruppuppgift.tasker.model.web.TeamWeb;
import se.group.backendgruppuppgift.tasker.repository.TeamRepository;
import se.group.backendgruppuppgift.tasker.repository.UserRepository;
import se.group.backendgruppuppgift.tasker.resource.converter.UserConverter;
import se.group.backendgruppuppgift.tasker.service.exception.InvalidUserException;
import se.group.backendgruppuppgift.tasker.service.exception.InvalidTeamException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.lang3.StringUtils.*;


@Service
public final class UserService {

    private final UserRepository repository;
    private  final TeamRepository teamRepository;
    private final TaskRepository taskRepository;

    public UserService(UserRepository repository, TeamRepository teamRepository, TaskRepository taskRepository) {
        this.repository = repository;
        this.teamRepository = teamRepository;
        this.taskRepository = taskRepository;
    }

    public User createUser(User user) {
        Long userNumber;
        Optional<User> optionalUser = repository.findFirstByOrderByUserNumberDesc();

        if(optionalUser.isPresent())
           userNumber = repository.findFirstByOrderByUserNumberDesc().get().getUserNumber();
        else
            userNumber = 1000L;

        AtomicLong number = new AtomicLong(userNumber);
        userNumber = number.incrementAndGet();

        user.setUserNumber(userNumber);
        user.setIsActive(true);

        if(repository.findUserByUsername(user.getUsername()).isPresent())
            throw new InvalidUserException("Username already in use");

        repository.save(user);
        return user;
    }

    public Optional<UserWeb> findUserByUserNumber(Long userNumber){
        Optional<User> user = repository.findByUserNumber(userNumber);
        if(user.isPresent()){
            Optional<UserWeb> userWeb = Optional.ofNullable(convertToWeb(user.get()));
            return userWeb;
        }
        return Optional.empty();
    }

    public Optional<UserWeb> deleteUserByUserNumber(Long userNumber){
        Optional<UserWeb> user = findUserByUserNumber(userNumber);
        if(user.isPresent()){
            repository.removeByUserNumber(userNumber);
        }
        return user;
    }

//    private UserWeb convertToWeb(User user) {
//        return new UserWeb(user.getUserNumber(),user.getUsername()
//                ,user.getFirstName(),user.getLastName()
//                , user.getIsActive(), user.getTeam());
//    }


    public User findLastUser(){
        return repository.findFirstByOrderByUserNumberDesc().get();
    }

    public List<User> findUsersByTeamId(Long teamId){
        return repository.findUsersByTeamId(teamId);
    }

    public User userActivator(Long userNumber){
        Optional<User> newUserOpt = repository.findByUserNumber(userNumber);
        User newUser = newUserOpt.get();
        if(newUser.getIsActive() == false){
            //Lägg till kod för att ta ta bort user från Task och ändra status till Unstarted
            List<Task> task = taskRepository.findAllByUserUserNumber(newUser.getUserNumber());
            for(Task t: task){
                t.setStatus(TaskStatus.UNSTARTED);
                t.setUser(null);
                taskRepository.save(t);
            }
        }
        return repository.save(newUser);
    }

    public Optional<User> updateUser(Long userNumber, UserWeb userWeb){
        //Optional<User> user = userWeb.getOptionalFromUser(userWeb);
        Optional<User> user = repository.findByUserNumber(userNumber);

        if(user.isPresent()){
            User updatedUser = user.get();
            if(!isBlank(userWeb.getFirstName()))
                updatedUser.setFirstName(userWeb.getFirstName());
            if(!isBlank(userWeb.getLastName()))
                updatedUser.setLastName(userWeb.getLastName());
            if (updatedUser.getIsActive() != null)
                updatedUser.setIsActive(userWeb.getIsActive());

            return Optional.ofNullable(repository.save(updatedUser));
            //return Optional.ofNullable(repository.save(UserConverter.getOptionalUserWeb(updatedUser)));
                    //Optional.ofNullable(userWeb.convertToWeb(repository.save(updatedUser)));
        }
        return Optional.empty();
    }
    /*
    public Optional<TaskWeb> updateTask(Long id, TaskWeb taskWeb) {
        validateTask(taskWeb);
        Optional<Task> task = taskRepository.findById(id);

        if (task.isPresent()) {
            Task updatedTask = task.get();
            if (!isBlank(taskWeb.getDescription()))
                updatedTask.setDescription(taskWeb.getDescription());

            if (!isBlank(taskWeb.getStatus().toString()))
                updatedTask.setStatus(taskWeb.getStatus());

            return Optional.ofNullable(convertToWeb(taskRepository.save(updatedTask)));
        }

        return Optional.empty();
    }
     */

    // -------------------------------TODO DENNA ÄR INTE KLAR.
    public Optional<TaskWeb> updateUserTask(Long userNumber, Long taskId){
        Optional<User> userResult = repository.findByUserNumber(userNumber);
        Optional<Task> taskResult = taskRepository.findById(taskId);

        if(taskResult.isPresent()){
            Task updatedTask = taskResult.get();
            User updatedUser = userResult.get();
            updatedTask.setUser(updatedUser);
            return Optional.ofNullable(convertTaskToWeb(taskRepository.save(updatedTask)));
        }
        return Optional.empty();
    }

    public List<User> findAllUsersBy(String firstName, String lastName, String userName){
        System.out.println(firstName + lastName + userName);
        if (!firstName.isEmpty() && lastName.isEmpty() && userName.isEmpty()){ // om allt är null förutom firstname
           return repository.findUsersByFirstName(firstName);
        }else if(firstName.isEmpty() && !lastName.isEmpty() && userName.isEmpty()){ // om allt är null förutom lastname
            return repository.findUsersByLastName(lastName);
        }else if(firstName.isEmpty() && lastName.isEmpty() && !userName.isEmpty()){ // om allt är null förutom username
            return repository.findUsersByUsername(userName);
        }else if (!firstName.isEmpty() && !lastName.isEmpty() && userName.isEmpty()){ // om allt är null förutom firsntame och lastname
            return repository.findUsersByFirstNameAndLastName(firstName,lastName);
        }else if (!firstName.isEmpty() && lastName.isEmpty() && !userName.isEmpty()){ // om allt är null förutom firstNAme och username
            return repository.findUsersByFirstNameAndUsername(firstName,userName);
        }else if(firstName.isEmpty() && !lastName.isEmpty() && !userName.isEmpty()){ // om firstname är null
            return repository.findUsersByUsernameAndLastName(userName, lastName);
        }else
            {return repository.findUsersByFirstNameAndLastNameAndUsername(firstName, lastName, userName);
        }
    }

    public Optional<TeamWeb> addTeam(Long id, UserWeb userWeb){
        Optional<User> result = repository.findByUserNumber(userWeb.getUserNumber());
        Optional<Team> resultTeam = teamRepository.findById(id);

        if(result.isPresent()){
            User user = result.get();
            userTeamValidation(user);

            if(resultTeam.isPresent()){
                Team team = resultTeam.get();
                maxUserLimitValidation(team);
                user.setTeam(team);

                repository.save(user);
                return Optional.ofNullable(convertToTeamWeb(team));
            }
        }
        return Optional.empty();
    }

    private void maxUserLimitValidation(Team team) {
        List<User> users = repository.findByTeam(team);

        if (users.size() >= 10) {
            throw new InvalidTeamException("Team: " + team.getName() + " is full, max 10 users in a team");
        }
    }

    private void userTeamValidation(User user) {

        if (user.getTeam() != null) {
            throw new InvalidTeamException("User: " + user.getUsername() + " is already in a team");
        }

    }

    private TeamWeb convertToTeamWeb(Team team) {
        return new TeamWeb(team.getId(), team.getName(), team.getIsActive());
    }

    private TaskWeb convertTaskToWeb(Task task) {
        return new TaskWeb(task.getId(), task.getDescription(), task.getStatus(), null);
    }

    private void checkUsername(User user){
        if(user.getFirstName() == null || user.getFirstName().length() <= 10){
            throw new InvalidUserException("FirstName needs to be 10 characters or longer!");
        }
    }
}

package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PostMapping;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<User, List<Message>> senderMap;
    private HashMap<Group, User> adminMap;
    // private HashSet<String> userMobile;
    private HashMap<String,User> userData;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<User,List<Message>>();
        this.adminMap = new HashMap<Group, User>();
        //this.userMobile = new HashSet<>();
        this.userData= new HashMap<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }
    public boolean isNewUser(String mobile){
        if(userData.containsKey(mobile)) return false;
        return true;
    }
    public void createUser(String name, String mobile) {
        userData.put(mobile,new User(name,mobile));
    }

    public Group createGroup(List<User> users){
        if(users.size()==2)
            return this.createPersonalChat(users);

        this.customGroupCount++;
        String groupName="Group "+this.customGroupCount;
        Group group= new Group(groupName,users.size());
        groupUserMap.put(group,users);
        adminMap.put(group,users.get(0));
        return group;
    }

    public Group createPersonalChat(List<User> users){
        String groupName=users.get(1).getName();
        Group personalGroup= new Group(groupName,2);
        groupUserMap.put(personalGroup,users);
        return personalGroup;
    }
    public int createMessage(String content){
        this.messageId++;
        Message message= new Message(messageId,content);
        return this.messageId;
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception{
        if(!groupUserMap.containsKey(group)){
            throw new Exception("Group does not exist");
        }
        if(!this.userExistsInGroup(group,sender)){
            throw new Exception("You are not allowed to send message");
        }
        List<Message> messages= new ArrayList<>();
        if(groupMessageMap.containsKey(group))
            messages=groupMessageMap.get(group);

        messages.add(message);
        groupMessageMap.put(group,messages);
        //mapping user and their send messages
        List<Message> messageList= new ArrayList<>();
        if(senderMap.containsKey(sender)){
            messageList=senderMap.get(sender);
            messageList.add(message);
        }
        senderMap.put(sender,messageList);

        return messages.size();
    }


    public String changeAdmin(User approver, User user, Group group) throws Exception{
        if(!groupUserMap.containsKey(group))
            throw new Exception("Group does not exist");
        if(!adminMap.get(group).equals(approver))
            throw new Exception("Approver does not have rights");
        if(!this.userExistsInGroup(group,user))
            throw new Exception("User is not a participant");

        adminMap.put(group,user);
        return "SUCCESS";
    }
    public boolean userExistsInGroup(Group group,User sender){
        List<User> users= groupUserMap.get(group);
        for(User user: users){
            if(user.equals(sender)) return true;
        }
        return false;
    }

    public int removeUser(User user) throws Exception{
        Set<Group> groups= groupUserMap.keySet();
        boolean checkUser=false;
        boolean checkAdmin=true;
        Group userGroup=null;
        for(Group group:groups){
            if(this.userExistsInGroup(group,user)){
                checkUser=true;
                userGroup=group;
                if(!adminMap.get(group).equals(user)){
                    groupUserMap.get(group).remove(user);
                    this.removeUserMessages(user,group);
                    senderMap.remove(user);
                    checkAdmin = false;
                }
            }
        }
        if(!checkUser) throw new Exception("User not found");
        else if(checkAdmin) throw new Exception("Cannot remove admin");
        else {
            int numOfUsersInGroup=groupUserMap.get(userGroup).size();
            int numOfMessagesInAllGroup=0;
            Set<Group> groupSet=groupMessageMap.keySet();
            for(Group group:groupSet){
                numOfMessagesInAllGroup += groupMessageMap.get(group).size();
            }
            return numOfMessagesInAllGroup+numOfUsersInGroup;
        }

    }
    public void removeUserMessages(User user,Group group){
        List<Message> messages=senderMap.get(user);
        //removing all its messages from group
        for(Message message:messages){
            groupMessageMap.get(group).remove(message);
        }
    }



//    public String findMessage(Date start, Date end, int K) throws Exception{
//        //This is a bonus problem and does not contains any marks
//        // Find the Kth latest message between start and end (excluding start and end)
//        // If the number of messages between given time is less than K, throw "K is greater than the number of messages" exception
//        return "found";
//    }

}
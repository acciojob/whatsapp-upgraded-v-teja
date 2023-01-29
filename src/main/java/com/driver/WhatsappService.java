package com.driver;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WhatsappService {

    WhatsappRepository whatsappRepository = new WhatsappRepository();
    public String createUser(String name, String mobile) throws Exception {
        //If the mobile number exists in database, throw "User already exists" exception
        //Otherwise, create the user and return "SUCCESS"
        try{
            if(whatsappRepository.getUserRepository().containsKey(mobile)){
                throw new Exception("User already exists");
            }else{
                User user = new User();
                user.setName(name);
                user.setMobile(mobile);
                whatsappRepository.getUserRepository().put(mobile,user);
            }
        }catch(Exception e){
            System.out.println(e);
        }
        return "SUCCESS";
    }

    public Group createGroup(List<User> users) throws Exception {
        // The list contains at least 2 users where the first user is the admin. A group has exactly one admin.
        // If there are only 2 users, the group is a personal chat and the group name should be kept as the name of the second user(other than admin)
        // If there are 2+ users, the name of group should be "Group count". For example, the name of first group would be "Group 1", second would be "Group 2" and so on.
        // Note that a personal chat is not considered a group and the count is not updated for personal chats.
        // If group is successfully created, return group.

        //For example: Consider userList1 = {Alex, Bob, Charlie}, userList2 = {Dan, Evan}, userList3 = {Felix, Graham, Hugh}.
        //If createGroup is called for these userLists in the same order, their group names would be "Group 1", "Evan", and "Group 2" respectively.

        int noOfUsers = users.size();
        Group group = new Group();
        try{
            if(noOfUsers<2){
                throw  new Exception("Not enough users to create a group");
            }else if(noOfUsers==2){
                group.setName(users.get(1).getName());
                group.setNumberOfParticipants(noOfUsers);
                whatsappRepository.getGroupRepository().put(group,users);
            }else{
                int groupCount = 0;
                for(Group temp : whatsappRepository.getGroupRepository().keySet()){
                    if(temp.getNumberOfParticipants()>2){
                        groupCount++;
                    }
                }
                groupCount++;
                String groupName = "Group "+groupCount;
                group.setName(groupName);
                group.setNumberOfParticipants(noOfUsers);
                whatsappRepository.getGroupRepository().put(group,users);
            }
        }catch (Exception e){
            System.out.println(e);
        }

        return group;
    }

    public int createMessage(String content) {
        // The 'i^th' created message has message id 'i'.
        // Return the message id.
        int noOfMessages = whatsappRepository.getMessageRepository().size();
        Message message = new Message();
        message.setId(noOfMessages+1);
        message.setContent(content);
        message.setTimestamp();
        whatsappRepository.getMessageRepository().put(message.getId(),message);

        return message.getId();
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception {
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "You are not allowed to send message" if the sender is not a member of the group
        //If the message is sent successfully, return the final number of messages in that group.
        try{
            if(!whatsappRepository.getGroupRepository().containsKey(group)){
                throw  new Exception("Group does not exist");
            }
            //check if sender is part of the group
            boolean flag = false;
            for(User user  : whatsappRepository.getGroupRepository().get(group)){
                if(user.equals(sender)){
                    flag =true;
                }
            }
            if(flag ==false){
                throw  new Exception("You are not allowed to send message");
            }

            message.setGroup(group);
            message.setUser(sender);
            sender.getMessageList().add(message);

            whatsappRepository.getUserRepository().put(sender.getName(),sender);

            group.getMessageList().add(message);
            List<User> users = whatsappRepository.getGroupRepository().get(group);
            whatsappRepository.getGroupRepository().put(group,users);
        }catch (Exception e){
            System.out.println(e);
        }
        return group.getMessageList().size();

    }

    public String changeAdmin(User approver, User user, Group group) throws Exception {
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "Approver does not have rights" if the approver is not the current admin of the group
        //Throw "User is not a participant" if the user is not a part of the group
        //Change the admin of the group to "user" and return "SUCCESS". Note that at one time there is only one admin and the admin rights
        if(!whatsappRepository.getGroupRepository().containsKey(group)){
            throw  new Exception("Group does not exist");
        }


        if(!whatsappRepository.getGroupRepository().get(group).get(0).equals(approver)){
            throw  new Exception("Approver does not have rights");
        }
        boolean flag = false;
        List<User> userList= whatsappRepository.getGroupRepository().get(group);
        int index =-1;
        for(User user1: userList){
            index++;
            if(user1.equals(user)){
                flag = true;
                break;
            }
        }
        if(flag==false){
            throw new Exception("User is not a participant");
        }

        User temp = userList.get(index);
        userList.set(index,userList.get(0));
        userList.set(0,temp);

        whatsappRepository.getGroupRepository().put(group,userList);

        return "SUCCESS";

    }

    public int removeUser(User user) throws Exception {
        //A user belongs to exactly one group
        //If user is not found in any group, throw "User not found" exception
        //If user is found in a group and it is the admin, throw "Cannot remove admin" exception
        //If user is not the admin, remove the user from the group, remove all its messages from all the databases, and update relevant attributes accordingly.
        //If user is removed successfully, return (the updated number of users in the group + the updated number of messages in group + the updated number of overall messages)
        boolean flag = false;
        int index=-1;
        Group userGroup = null;
        for(Group group: whatsappRepository.getGroupRepository().keySet()){
            for(User user1: whatsappRepository.getGroupRepository().get(group)){
                index++;
                if(user.equals(user1)){
                    userGroup = group;
                    flag = true;
                    break;
                }
            }
            if(flag==true){
                break;
            }
            index = -1;
        }

        if(flag==false){
            throw  new Exception("User not found");
        }
        if(index==0){
            throw new Exception("Cannot remove admin");
        }else{
            //remove user from group list
            List<User> updatedUserList = new ArrayList<>();
            List<User> userLIst = whatsappRepository.getGroupRepository().get(userGroup);
            for(User updatedUsers: userLIst){
                if(updatedUsers.equals(user))
                    continue;
                updatedUserList.add(updatedUsers);
            }
            whatsappRepository.getGroupRepository().put(userGroup,updatedUserList);

            //removing user from user repo
            whatsappRepository.getUserRepository().remove(user.getName());

            userGroup.getMessageList().removeIf(message -> message.getUser().equals(user));
            //remove user messages from group message list
            List<Message> messageList = userGroup.getMessageList();
            List<Message> updatedMessageList = new ArrayList<>();
            for(Message updatedMessage : messageList){
                if(updatedMessage.getUser().equals(user) && updatedMessage.getGroup().equals(userGroup))
                    continue;
                updatedMessageList.add(updatedMessage);
            }
            userGroup.setMessageList(updatedMessageList);
            whatsappRepository.getGroupRepository().put(userGroup,updatedUserList);

            for(int i: whatsappRepository.getMessageRepository().keySet()){
                if(whatsappRepository.getMessageRepository().get(i).getUser().equals(user) && whatsappRepository.getMessageRepository().get(i).getGroup().equals(userGroup)){
                    whatsappRepository.getMessageRepository().remove(i);
                }
            }

        }

        int noOfUsers = whatsappRepository.getGroupRepository().get(userGroup).size();
        int noOfMessagesInGroup = userGroup.getMessageList().size();
        int overallMessages = whatsappRepository.getMessageRepository().size();

        return noOfUsers+noOfMessagesInGroup+ overallMessages;
    }

    public String findMessage(Date start, Date end, int k) throws Exception {

        // This is a bonus problem and does not contains any marks
        // Find the Kth latest message between start and end (excluding start and end)
        // If the number of messages between given time is less than K, throw "K is greater than the number of messages" exception

        List<Message> messageList = new ArrayList<>();
        for(Message message : whatsappRepository.getMessageRepository().values()){
            if(message.getTimestamp().compareTo(start)>0 && message.getTimestamp().compareTo(end)<0){
                messageList.add(message);
            }
        }
        Comparator<Message> compareByDate = (Message m1, Message m2) -> m1.getTimestamp().compareTo(m2.getTimestamp());

        Collections.sort(messageList,compareByDate);

        if(messageList.size()<k){
            throw new Exception("K is greater than the number of messages");
        }else{
            return messageList.get(k-1).getContent();
        }

    }
}

package com.driver;

import java.util.*;

//@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private int customGroupCount;
    private int messageId;

    //This is an extra variable
//    private int numberOfSenders;

    //=============================================================================================================


    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 0;
        this.messageId = 0;
//        this.numberOfSenders = 0;
    }

    //=============================================================================================================
    //=============================================================================================================

    public String createUser(String name, String mobile) throws Exception {
        if(userMobile.contains(mobile)){
            throw new Exception("User already exists");
        }
        userMobile.add(mobile);
        User user = new User(name, mobile);
        return "SUCCESS";
    }

    //=============================================================================================================
    //=============================================================================================================

    public Group createGroup(List<User> users){
        int count = users.size();

        //Creating the group : Deciding name of the group according to the numbers of participants(users)
        Group group;
        if(count==2){
            group = new Group(users.get(1).getName(), count);
        }
        else {
            customGroupCount++;
            group = new Group("Group " + customGroupCount, count);
        }

        //Mapping "group admin (first user of the provided list)" with the "current group"
        adminMap = new HashMap<>();
        adminMap.put(group,users.get(0));

        //Mapping user list with group
        groupUserMap = new HashMap<>();
        groupUserMap.put(group,users);


        return group;
    }

    //=============================================================================================================
    //=============================================================================================================

    public int createMessage(String content){
        messageId++;
        Message message = new Message(messageId, content, new Date());
        return messageId;
    }

    //=============================================================================================================
    //=============================================================================================================

    public int sendMessage(Message message, User sender, Group group) throws Exception {
        if(!groupUserMap.containsKey(group)){
            throw new Exception("Group does not exist");
        }
        boolean flag = false;
        for(User user : groupUserMap.get(group)){
            if (user.getMobile().equals(sender.getMobile())) {
                flag = true;
                break;
            }
        }
        if(!flag){
            throw new Exception("You are not allowed to send message");
        }

        Iterator<Map.Entry<Message,User>> iterator = senderMap.entrySet().iterator();
        boolean whetherSenderHasSentAnyMessageTillNow = false;
        while(iterator.hasNext()){
            if(iterator.next().getValue().getMobile().equals(sender.getMobile())){
                whetherSenderHasSentAnyMessageTillNow = true;
                break;
            }
        }
//        if(!whetherSenderHasSentAnyMessageTillNow){
//            numberOfSenders++;
//        }

        senderMap.put(message,sender);

        List<Message>messageList;
        if(groupMessageMap.containsKey(group)){
            messageList = groupMessageMap.get(group);
            messageList.add(message);
        }
        else {
            messageList = new ArrayList<>();
            messageList.add(message);
            groupMessageMap.put(group,messageList);
        }
        return messageList.size();
    }

    //=============================================================================================================
    //=============================================================================================================

    public String changeAdmin(User approver, User user, Group group) throws Exception{
        if(!groupUserMap.containsKey(group)){
            throw new Exception("Group does not exist");
        }
        if(!adminMap.get(group).equals(approver)){
            throw new Exception("Approver does not have rights");
        }

        List<User> userList = groupUserMap.get(group);
        boolean flag = false;
        for(User currentuser : userList){
            if(currentuser.equals(user)){
                flag = true;
                break;
            }
        }
        if(!flag){
            throw new Exception("User is not a participant");
        }

        adminMap.put(group,user);
        return "SUCCESS";

    }

    //=============================================================================================================
    //=============================================================================================================

    public int removeUser(User user) throws Exception{
//        First we will check whether the user is present in any group or not
        Group usersGroup = null;
        boolean flag = false;
        for(Group group : groupUserMap.keySet()){
            for(User currentUser : groupUserMap.get(group)){
                if(currentUser.getMobile().equals(user.getMobile())){
                    flag = true;
                    usersGroup = group;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        if(!flag){
            throw new Exception("User not found");
        }
        //_______________________________________________________________________________________

        //Now we will check whether the user is admin because we cannot remove an admin from the group :)
        if(adminMap.get(usersGroup).getMobile().equals(user.getMobile())){
            throw new Exception("Cannot remove admin");
        }


        //_______________________________________________________________________________________
        //Ignore this part
        System.out.println(" total parti : " + usersGroup.getNumberOfParticipants() +
                " total messages in group : " + groupMessageMap.get(usersGroup).size() +
                " total senders : " + senderMap.size());
        //_______________________________________________________________________________________

        //Now we have to remove all his messages from the
        //group. And one more thing, since a user can belong to one and only one group, so we will have to remove his messages
        //from that particular group only, and we don't have to worry about other groups.

        Iterator<Message> iterator = groupMessageMap.get(usersGroup).iterator(); //List of messages

        while(iterator.hasNext()){
            Message message = iterator.next();
            if(senderMap.containsKey(message)){
                if(senderMap.get(message).getMobile().equals(user.getMobile())){
                    iterator.remove();
                }
            }
        }

        for(Message message1 : groupMessageMap.get((usersGroup))){
            System.out.println(message1.getContent());
        }
        System.out.println(groupMessageMap.get(usersGroup).size());

//        Iterator<Message> iterator1 = senderMap.keySet().iterator();
        //Here we will not use simple keyset iteration because we need access to value also so we will use
        //entrySet
        Iterator<Map.Entry<Message,User>> iterator1 = senderMap.entrySet().iterator();
        while(iterator1.hasNext()) {
            if(iterator1.next().getValue().getMobile().equals(user.getMobile())) {
                iterator1.remove();
            }
        }

        System.out.println(senderMap.size());

        List<User> users = groupUserMap.get(usersGroup);
        users.remove(user);
        //Now here above we have removed the user from the group

        //We will have to update the number of participants present in the group (decrease them by 1)
        usersGroup.setNumberOfParticipants(usersGroup.getNumberOfParticipants()-1);

        System.out.println(usersGroup.getNumberOfParticipants());

        //If user is removed successfully, return (the updated number of users in the group +
        //the updated number of messages in group + the updated number of overall messages)
        return groupUserMap.get(usersGroup).size() + groupMessageMap.get(usersGroup).size() + senderMap.size();
    }

    //=============================================================================================================
    //=============================================================================================================

    public String findMessage(Date start, Date end, int K) throws Exception {
        //TreeSet<Message> messages = new TreeSet<>(Comparator.comparing(Message::getTimestamp));
        TreeSet<Message> messages = new TreeSet<>((messA,messB) -> messA.getTimestamp().compareTo(messB.getTimestamp()));

        //Every group has some messages and we will check all those messages that whether they lie in the
        //specified Date range or not (Here Date implies Time and Date both).
        //If they lie in the range we will put them in the TreeSet (Here I have used TreeSet to maintain the sorted order)

        //____________________________________________________________________________________________________________

        //We will iterate over each and every group present in the groupMessageMap
        for(Group group : groupMessageMap.keySet()){
            for(Message currentMessage : groupMessageMap.get(group)){
                if(currentMessage.getTimestamp().after(start) && currentMessage.getTimestamp().before(end)){
                    messages.add(currentMessage);
                }
            }
        }
        //Now till here we have filled our TreeSet with the appropriate messages which lie in a particular Date range
        //____________________________________________________________________________________________________________

        if(messages.size()<K){
            throw new Exception("K is greater than the number of messages");
        }

        //K==1 last message

        Iterator<Message> iterator = messages.descendingIterator();
        while(iterator.hasNext()){
            if(K-1>0){
                K--;
                iterator.next();
            }
            else {
                break;
            }
        }
        return iterator.next().getContent();


    }
}

//=============================================================================================================
//=============================================================================================================
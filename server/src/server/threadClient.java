/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import command.CommandList;
import command.Message;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Muhammad Izzuddin
 */
public class threadClient implements Runnable {

    private Socket sockcli;
    private final ArrayList<threadClient> alThread;
    private DataInputStream dis = null;
    private DataOutputStream dos = null;
    public ObjectOutputStream oos = null;
    private ObjectInputStream ois = null;
    private final SocketAddress sa;
    public String username;
    private String roomname = "KOSONG";
    private FileInputStream fis = null;
    private FileOutputStream fos = null;
    private ArrayList<String> Recipient = new ArrayList<>();
    private threadServer server;
    public boolean flag = false;
    private int score;
    public threadClient(threadServer server, Socket sockcli, ArrayList<threadClient> t) {
        this.server = server;
        this.sockcli = sockcli;
        this.alThread = t;
        this.sa = this.sockcli.getRemoteSocketAddress();
        this.score = 0;
    }

    @Override
    public void run() {
        try {
            dos = new DataOutputStream(this.getSockcli().getOutputStream());
            oos = new ObjectOutputStream(this.getSockcli().getOutputStream());
            dis = new DataInputStream(this.getSockcli().getInputStream());
            ois = new ObjectInputStream(this.getSockcli().getInputStream());

            String msg;
            while (true) {
                Object recv;
                try {
                    recv = ois.readObject();
                    if (recv instanceof Message) {
                        Message baru = (Message) recv;
                        sendMultiple(baru, getRoomname());
                    }
                    if (recv instanceof command.CommandList) {
                        command.CommandList comm = (command.CommandList) recv;
                        msg = comm.getCommand();

                        command.CommandList baru = new CommandList();
                        if (msg.equals("END")) {    
                            //server.updateUserList();
                            break;
                        } 
                        else if (msg.equals("START")) {
                            System.out.println("START");
                            boolean flagExist = false;
                            for (int i = 0; i < alThread.size(); i++) {
                                if (comm.getCommandDetails().get(0).equals(alThread.get(i).getUsername())) {
                                    baru.setCommand("EXIST");
                                    send(baru);
                                    flagExist = true;
                                    break;
                                }
                            }

                            if (flagExist == false) {
                                username = comm.getCommandDetails().get(0);
                                baru.setCommand("ROOMLIST");
                                baru.setCommandDetails(server.getRoomList());
                                send(baru);
                            }

                            //sendWord();-- ke setelah 
                        } 
                        else if (msg.equals("FIN")) {
                            if (comm.getCommandDetails().get(0).equals(server.getCurrentWord(getRoomname()))) {
                                server.tS.countAll.put(roomname, 30);
                                System.out.println("FINISH " + comm.getCommandDetails().get(1));
                                server.changeCurrentWord(getRoomname());
                                updateWord(getRoomname());
                                Message notif = new Message();
                                notif.setDari("Pemberitahuan");
                                this.score += 10;
                                notif.setIsi(comm.getCommandDetails().get(1) + " Menang");
                                System.out.println(username+' '+score);
                                sendMultiple(notif, getRoomname());
                                baru.setCommand("SCORE");
                                ArrayList<String> s = new ArrayList();
                                s.add(score+"");
                                baru.setCommandDetails(s);
                                send(baru);
                                updateTop();
                            }
                        }
                        
                        else if(msg.equals("RNAME")){
                            String oldroomname = getRoomname();
                            if (oldroomname == "") oldroomname = "KOSONG";
                            setRoomname(comm.getCommandDetails().get(0));
                            System.out.println("set" + username + "room" + getRoomname());
                            sendWord();
                            server.updateUserList();
                            updateTop();
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(threadClient.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(threadClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            synchronized (this.oos){
                oos.close();
                ois.close();
            }
            synchronized (this.sockcli){
                getSockcli().close();
            }
            synchronized (this.alThread) {
                this.alThread.remove(this);
            }
        } catch (IOException ex) {
            Logger.getLogger(threadClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public synchronized void sendMultiple(Object msg, String roomnamecari) throws IOException {
        for (int i = 0; i < this.alThread.size(); i++) {
            threadClient tc = this.alThread.get(i);
            if (tc.getRoomname().equals(roomnamecari))
                tc.send(msg);
        }
    }

    public synchronized void updateWord(String roomnamecari) throws IOException {
        for (int i = 0; i < this.alThread.size(); i++) {
            threadClient tc = this.alThread.get(i);
            if (tc.getRoomname().equals(roomnamecari))
                   tc.sendWord();
        }
    }

    public void send(Object msg) {
        try {
            this.oos.writeObject(msg);
            this.oos.flush();
            this.oos.reset();
        } catch (IOException ex) {
            Logger.getLogger(threadClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendWord() {
        command.CommandList baru = new command.CommandList();
        baru.setCommand("WORDS");
        ArrayList<String> detail = new ArrayList<>();
        detail.add(server.getCurrentWord(getRoomname()));
        detail.add((Integer.toString(server.tS.countAll.get(roomname))));
        baru.setCommandDetails(detail);
        send(baru);
    }
    
    public void updateTop(){
        List<Score> scores = new ArrayList<Score>();
        for (int i=0;i<alThread.size();i++){
            if (alThread.get(i).getRoomname() != "KOSONG")
                scores.add(new Score(alThread.get(i).score,alThread.get(i).username));
        }       
        Collections.sort(scores);
        CommandList baru = new CommandList();
        baru.setCommand("TOP5");
        ArrayList<String> top = new ArrayList<>();
        if (scores.size()>= 5){
            for (int i=0;i<5;i++){
                top.add(Integer.toString(scores.get(i).score));
                top.add(scores.get(i).name);
            }
        }
        else{
            for (int i=0;i<scores.size();i++){
                top.add(Integer.toString(scores.get(i).score));
                top.add(scores.get(i).name);
            }
        }
            
        baru.setCommandDetails(top);
        for(int i=0;i<server.roomList.size();i++){
            try {
                sendMultiple(baru, server.roomList.get(i));
            } catch (IOException ex) {
                Logger.getLogger(threadClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    /**
     * @return the sockcli
     */
    public Socket getSockcli() {
        return sockcli;
    }

    /**
     * @param sockcli the sockcli to set
     */
    public void setSockcli(Socket sockcli) {
        this.sockcli = sockcli;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the roomname
     */
    public String getRoomname() {
        return roomname;
    }

    /**
     * @param roomname the roomname to set
     */
    public void setRoomname(String roomname) {
        this.roomname = roomname;
    }
    
    class Score implements Comparable<Score> {
        int score;
        String name;

        public Score(int score, String name) {
            this.score = score;
            this.name = name;
        }

        @Override
        public int compareTo(Score o) {
            return score > o.score ? -1 : score < o.score ? 1 : 0;
        }
    }
}

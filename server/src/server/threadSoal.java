/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server;

import java.util.HashMap;
import java.util.TimerTask;
import java.util.Timer;

/**
 *
 * @author Nata
 */
public class threadSoal extends Thread{
    threadServer tS;
    public HashMap <String,Integer> countAll = new HashMap<>();
    public Timer baru;
    public Timer updatelist;
    @Override
    public void run(){
        for(int i=0; i<tS.roomList.size(); i++){
            countAll.put(tS.roomList.get(i),30);
        }
        //System.out.println(countAll.get("Software"));
        startWaktu();
        
    }
    public void startWaktu(){
        baru = new Timer();
        baru.schedule(new SayHello(this,countAll), 0,1000);
    }
    public void startUpdate(){
        updatelist = new Timer();
        updatelist.schedule(new update(this), 0,10000);
    }
    
    public threadSoal(threadServer tSr){
        this.tS = tSr;
    }
    /**
     * @return the count
     */
}

class SayHello extends TimerTask {
    private threadSoal t;
    private HashMap<String,Integer> baru = new HashMap<String, Integer>();
    SayHello(threadSoal t, HashMap<String,Integer> baru){
        this.t = t;
        this.baru = baru;
    }
    public void run() {
       
        for(int i=0; i<t.tS.roomList.size(); i++){
            if ((baru.get(t.tS.roomList.get(i))) == 0){
                baru.put(t.tS.roomList.get(i),30);
                t.tS.changeCurrentWord(t.tS.roomList.get(i));
                t.tS.sendWord(t.tS.roomList.get(i));
            }
        }
        System.out.println(baru.get("Software"));
        for(int i=0; i<t.tS.roomList.size(); i++){
            baru.put(t.tS.roomList.get(i), baru.get(t.tS.roomList.get(i))-1);
        }
        //t.setCount(t.getCount()+1);
        
    }
    
 }

class update extends TimerTask {
    private threadSoal t;
    update(threadSoal t){
        this.t = t;
    }
    public void run() {
        t.tS.updateUserList();
    }    
 }
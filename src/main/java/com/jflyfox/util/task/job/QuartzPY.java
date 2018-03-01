package com.jflyfox.util.task.job;


public class QuartzPY {


    public void runPYtrigger() {
            try {
                System.out.println("【定时任务启动】开始()...");
                QuartzManager.addJob("py", SpiderJob.class, "*/5 * * * * ?","2");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

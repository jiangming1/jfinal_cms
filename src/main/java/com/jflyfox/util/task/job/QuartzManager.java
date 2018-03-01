package com.jflyfox.util.task.job;

import cn.dreampie.quartz.job.QuartzJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class QuartzManager {
    private static SchedulerFactory gSchedulerFactory = new StdSchedulerFactory();
    private static String JOB_GROUP_NAME = "GROUP";

    /**
     * 添加一个定时任务，使用默认的任务组名，触发器名，触发器组名
     * @param jobName 任务名
     * @param cls 任务
     * @param time 时间设置
     * @param configId 定时器ID
     */
    public static void addJob(String jobName, Class cls, String time ,String configId) {
        try {
//            Scheduler sched = gSchedulerFactory.getScheduler();
//            // 任务名，任务组，任务执行类
//            JobDetail jobDetail = JobBuilder.newJob(SpiderJob.class).withIdentity(jobName, JOB_GROUP_NAME).build();
//            jobDetail.getJobDataMap().put("quartzJob",new SpiderJob());
//            //可以传递参数
//            if(null!= configId){
//                jobDetail.getJobDataMap().put("triggerId", jobName);
//                jobDetail.getJobDataMap().put("configId", configId);
//            }
//            // 触发器
//            CronTrigger trigger = newTrigger().withIdentity(jobName, JOB_GROUP_NAME).withSchedule(cronSchedule(time)).build();
//            // 触发器时间设定
//            sched.scheduleJob(jobDetail, trigger);
//            // 启动
//            if (!sched.isShutdown()) {
//                sched.start();
//            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 修改一个任务的触发时间(使用默认的任务组名，触发器名，触发器组名)
     * @param jobName
     * @param time
     * @param triggerId 定时器ID
     */
    @SuppressWarnings("rawtypes")
    public static void modifyJobTime(String jobName, String time,String triggerId) {
        try {
            //String newTime = getCron(time);
            Scheduler sched = gSchedulerFactory.getScheduler();
            TriggerKey triggerKey = new TriggerKey(jobName,JOB_GROUP_NAME);
            JobKey jobKey = new JobKey(jobName,JOB_GROUP_NAME);
            CronTrigger trigger = (CronTrigger) sched.getTrigger(triggerKey);

            if (trigger == null) {
                //if(time.after(new Date())){
                if (!getCron(new Date()).equalsIgnoreCase(time)) {
                    addJob(jobName, QuartzJob.class, time,triggerId);
                }
                return;
            }
            String oldTime = String.valueOf(trigger.getCronExpression());
           // if (!oldTime.equalsIgnoreCase(time)) {
                JobDetail jobDetail = sched.getJobDetail(jobKey);
                Class objJobClass = jobDetail.getJobClass();
                removeJob(jobName);
                addJob(jobName, objJobClass, time,triggerId);
               // System.out.println(jobName + "修改定时器时间"+ time);
            //}
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 移除一个任务(使用默认的任务组名，触发器名，触发器组名)
     * @param jobName
     */
    public static void removeJob(String jobName) {
        try {
            TriggerKey triggerKey = new TriggerKey(jobName,JOB_GROUP_NAME);
            JobKey jobKey = new JobKey(jobName,JOB_GROUP_NAME);
            Scheduler sched = gSchedulerFactory.getScheduler();
            sched.pauseTrigger(triggerKey);// 停止触发器
            sched.unscheduleJob(triggerKey);// 移除触发器
            sched.deleteJob(jobKey);// 删除任务
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 启动所有定时任务
     */
    public static void startJobs() {
        try {
            Scheduler sched = gSchedulerFactory.getScheduler();
            sched.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 关闭所有定时任务
     */
    public static void shutdownJobs() {
        try {
            Scheduler sched = gSchedulerFactory.getScheduler();
            if (!sched.isShutdown()) {
                sched.shutdown();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static String formatDateByPattern(Date date, String dateFormat){
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        String formatTimeStr = null;
        if (date != null) {
            formatTimeStr = sdf.format(date);
        }
        return formatTimeStr;
    }
    public static String getCron(Date  date){
        String dateFormat="ss mm HH dd MM ? yyyy";
        return formatDateByPattern(date, dateFormat);
    }
}

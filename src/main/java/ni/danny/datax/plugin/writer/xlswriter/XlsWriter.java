package ni.danny.datax.plugin.writer.xlswriter;


import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * @author bingobing
 */
public class XlsWriter extends Writer {
    public static class Job extends Writer.Job{
        private static final Logger LOG = LoggerFactory.getLogger(Job.class);
        private Configuration writerSliceConfig = null;

        @Override
        public void init() {
            this.writerSliceConfig = this.getPluginJobConf();
            this.validateParameter();

        }

        private void validateParameter(){
            this.writerSliceConfig.getNecessaryValue(Key.FILE_NAME,
                    XlsWriterErrorCode.REQUIRED_VALUE);
            String path = this.writerSliceConfig.getNecessaryValue(Key.PATH,
                    XlsWriterErrorCode.REQUIRED_VALUE);
            try{
               File dir = new File(path);
               if(dir.isFile()){
                   throw DataXException.asDataXException(XlsWriterErrorCode.ILLEGAL_VALUE
                           ,String.format("您配置的path: [%s] 不是一个合法的目录, 请您注意文件重名, 不合法目录名等情况.",path));
               }
               if(!dir.exists()){
                   boolean createdOk = dir.mkdirs();
                   if (!createdOk) {
                       throw DataXException
                               .asDataXException(
                                       XlsWriterErrorCode.CONFIG_INVALID_EXCEPTION,
                                       String.format("您指定的文件路径 : [%s] 创建失败.",
                                               path));
                   }
               }
            }catch (SecurityException se){
                throw DataXException.asDataXException(
                        XlsWriterErrorCode.SECURITY_NOT_ENOUGH,
                        String.format("您没有权限创建文件路径 : [%s] ", path), se);
            }

        }

        @Override
        public void prepare() {
            String path = this.writerSliceConfig.getString(Key.PATH);
            String fileName = this.writerSliceConfig
                    .getString(Key.FILE_NAME);
            String writeMode = this.writerSliceConfig
                    .getString(Key.WRITE_MODE);
            // truncate option handler
            if ("truncate".equals(writeMode)) {
                LOG.info(String.format(
                        "由于您配置了writeMode truncate, 开始清理 [%s] 下面以 [%s] 开头的内容",
                        path, fileName));
                File dir = new File(path);
                // warn:需要判断文件是否存在，不存在时，不能删除
                try {
                    if (dir.exists()) {
                        // warn:不要使用FileUtils.deleteQuietly(dir);
                        FilenameFilter filter = new PrefixFileFilter(fileName);
                        File[] filesWithFileNamePrefix = dir.listFiles(filter);
                        for (File eachFile : filesWithFileNamePrefix) {
                            LOG.info(String.format("delete file [%s].",
                                    eachFile.getName()));
                            FileUtils.forceDelete(eachFile);
                        }
                        // FileUtils.cleanDirectory(dir);
                    }
                } catch (NullPointerException npe) {
                    throw DataXException
                            .asDataXException(
                                    XlsWriterErrorCode.Write_FILE_ERROR,
                                    String.format("您配置的目录清空时出现空指针异常 : [%s]",
                                            path), npe);
                } catch (IllegalArgumentException iae) {
                    throw DataXException.asDataXException(
                            XlsWriterErrorCode.SECURITY_NOT_ENOUGH,
                            String.format("您配置的目录参数异常 : [%s]", path));
                } catch (SecurityException se) {
                    throw DataXException.asDataXException(
                            XlsWriterErrorCode.SECURITY_NOT_ENOUGH,
                            String.format("您没有权限查看目录 : [%s]", path));
                } catch (IOException e) {
                    throw DataXException.asDataXException(
                            XlsWriterErrorCode.Write_FILE_ERROR,
                            String.format("无法清空目录 : [%s]", path), e);
                }
            } else if ("append".equals(writeMode)) {
                LOG.info(String
                        .format("由于您配置了writeMode append, 写入前不做清理工作, [%s] 目录下写入相应文件名前缀  [%s] 的文件",
                                path, fileName));
            } else if ("nonConflict".equals(writeMode)) {
                LOG.info(String.format(
                        "由于您配置了writeMode nonConflict, 开始检查 [%s] 下面的内容", path));
                // warn: check two times about exists, mkdirs
                File dir = new File(path);
                try {
                    if (dir.exists()) {
                        if (dir.isFile()) {
                            throw DataXException
                                    .asDataXException(
                                            XlsWriterErrorCode.ILLEGAL_VALUE,
                                            String.format(
                                                    "您配置的path: [%s] 不是一个合法的目录, 请您注意文件重名, 不合法目录名等情况.",
                                                    path));
                        }
                        // fileName is not null
                        FilenameFilter filter = new PrefixFileFilter(fileName);
                        File[] filesWithFileNamePrefix = dir.listFiles(filter);
                        if (filesWithFileNamePrefix.length > 0) {
                            List<String> allFiles = new ArrayList<String>();
                            for (File eachFile : filesWithFileNamePrefix) {
                                allFiles.add(eachFile.getName());
                            }
                            LOG.error(String.format("冲突文件列表为: [%s]",
                                    StringUtils.join(allFiles, ",")));
                            throw DataXException
                                    .asDataXException(
                                            XlsWriterErrorCode.ILLEGAL_VALUE,
                                            String.format(
                                                    "您配置的path: [%s] 目录不为空, 下面存在其他文件或文件夹.",
                                                    path));
                        }
                    } else {
                        boolean createdOk = dir.mkdirs();
                        if (!createdOk) {
                            throw DataXException
                                    .asDataXException(
                                            XlsWriterErrorCode.CONFIG_INVALID_EXCEPTION,
                                            String.format(
                                                    "您指定的文件路径 : [%s] 创建失败.",
                                                    path));
                        }
                    }
                } catch (SecurityException se) {
                    throw DataXException.asDataXException(
                            XlsWriterErrorCode.SECURITY_NOT_ENOUGH,
                            String.format("您没有权限查看目录 : [%s]", path));
                }
            } else {
                throw DataXException
                        .asDataXException(
                                XlsWriterErrorCode.ILLEGAL_VALUE,
                                String.format(
                                        "仅支持 truncate, append, nonConflict 三种模式, 不支持您配置的 writeMode 模式 : [%s]",
                                        writeMode));
            }
        }

        @Override
        public List<Configuration> split(int mandatoryNumber) {
            LOG.info("begin do split...");
            List<Configuration> writerSplitConfigs = new ArrayList<Configuration>();
            String filePrefix = this.writerSliceConfig
                    .getString(Key.FILE_NAME);

            Set<String> allFiles = new HashSet<String>();
            String path = null;
            try {
                path = this.writerSliceConfig.getString(Key.PATH);
                File dir = new File(path);
                allFiles.addAll(Arrays.asList(dir.list()));
            } catch (SecurityException se) {
                throw DataXException.asDataXException(
                        XlsWriterErrorCode.SECURITY_NOT_ENOUGH,
                        String.format("您没有权限查看目录 : [%s]", path));
            }

            String fileSuffix;
            for (int i = 0; i < mandatoryNumber; i++) {
                // handle same file name

                Configuration splitedTaskConfig = this.writerSliceConfig
                        .clone();

                String fullFileName = null;
                fileSuffix = UUID.randomUUID().toString().replace('-', '_');
                fullFileName = String.format("%s__%s", filePrefix, fileSuffix);
                while (allFiles.contains(fullFileName)) {
                    fileSuffix = UUID.randomUUID().toString().replace('-', '_');
                    fullFileName = String.format("%s__%s", filePrefix,
                            fileSuffix);
                }
                allFiles.add(fullFileName);

                splitedTaskConfig
                        .set(Key.FILE_NAME,fullFileName);

                LOG.info(String.format("splited write file name:[%s]",
                        fullFileName));

                writerSplitConfigs.add(splitedTaskConfig);
            }
            LOG.info("end do split.");
            return writerSplitConfigs;
        }



        @Override
        public void destroy() {

        }
    }

    public static class Task extends Writer.Task{
        private static final Logger LOG = LoggerFactory.getLogger(Task.class);

        private Configuration writerSliceConfig;

        private String path;

        private String fileName;

        private String dateFormat;

        private String fileType;

        private List<String> fields;

        private int fieldNum = 0;

        @Override
        public void init() {
            this.writerSliceConfig = this.getPluginJobConf();
            this.path = this.writerSliceConfig.getString(Key.PATH);
            this.dateFormat = this.writerSliceConfig.getString(Key.DATE_FORMAT);
            this.fileName = this.writerSliceConfig.getString(Key.FILE_NAME);
            this.fields = this.writerSliceConfig.getList(Key.FIELDS,String.class);
            this.fieldNum = this.fields.size();
            this.fileType = this.writerSliceConfig.getString(Key.FILE_TYPE);

        }

        @Override
        public void startWrite(RecordReceiver lineReceiver) {
            LOG.info("begin do write...");
            String fileFullPath = this.buildFilePath();
            LOG.info(String.format("write to file : [%s]", fileFullPath));
            ExcelWriter excelWriter = null;
            try {
                excelWriter = EasyExcel.write(fileName).head(sheetHead()).build();
                WriteSheet writeSheet = EasyExcel.writerSheet("sheet").build();
                // 去调用写入,这里我调用了五次，实际使用时根据数据库分页的总的页数来
                excelWriter.write(sheetData(lineReceiver), writeSheet);
            } catch (SecurityException se) {
                throw DataXException.asDataXException(
                        XlsWriterErrorCode.SECURITY_NOT_ENOUGH,
                        String.format("您没有权限创建文件  : [%s]", this.fileName));
            } finally {
                if (excelWriter != null) {
                    excelWriter.finish();
                }
            }
            LOG.info("end do write");
        }

        private List<List<String>> sheetHead() {
            List<List<String>> list = new ArrayList<List<String>>();
            for(String title:this.fields){
                List<String> head = new ArrayList<String>();
                head.add(title);
                list.add(head);
            }
            return list;
        }

        private List<List<String>> sheetData(RecordReceiver lineReceiver){
            List<List<String>> list = new ArrayList<>();
            List<String> dataList = new ArrayList<>();
            for (int i = 0; i < lineReceiver.getFromReader().getColumnNumber(); i++) {
                String data = lineReceiver.getFromReader().getColumn(i).getRawData().toString();
                dataList.add(data);
            }
            list.add(dataList);
            return list;
        }

        private String buildFilePath() {
            boolean isEndWithSeparator = false;
            switch (IOUtils.DIR_SEPARATOR) {
                case IOUtils.DIR_SEPARATOR_UNIX:
                    isEndWithSeparator = this.path.endsWith(String
                            .valueOf(IOUtils.DIR_SEPARATOR));
                    break;
                case IOUtils.DIR_SEPARATOR_WINDOWS:
                    isEndWithSeparator = this.path.endsWith(String
                            .valueOf(IOUtils.DIR_SEPARATOR_WINDOWS));
                    break;
                default:
                    break;
            }
            if (!isEndWithSeparator) {
                this.path = this.path + IOUtils.DIR_SEPARATOR;
            }
            return String.format("%s%s", this.path, this.fileName);
        }


        @Override
        public void destroy() {

        }
    }
}

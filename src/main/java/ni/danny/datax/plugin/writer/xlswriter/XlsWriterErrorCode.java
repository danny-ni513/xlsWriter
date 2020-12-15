package ni.danny.datax.plugin.writer.xlswriter;

import com.alibaba.datax.common.spi.ErrorCode;

public enum XlsWriterErrorCode implements ErrorCode {

    CONFIG_INVALID_EXCEPTION("XlsWriter-00", "您的参数配置错误."),
    REQUIRED_VALUE("XlsWriter-01", "您缺失了必须填写的参数值."),
    ILLEGAL_VALUE("XlsWriter-02", "您填写的参数值不合法."),
    Write_FILE_ERROR("XlsWriter-03", "您配置的目标文件在写入时异常."),
    Write_FILE_IO_ERROR("XlsWriter-04", "您配置的文件在写入时出现IO异常."),
    SECURITY_NOT_ENOUGH("XlsWriter-05", "您缺少权限执行相应的文件写入操作.");

    private final String code;
    private final String description;

    private XlsWriterErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return String.format("Code:[%s], Description:[%s].", this.code,
                this.description);
    }

}

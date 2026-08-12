CREATE INDEX idx_video_timeline ON video (status, published_at DESC, id DESC)
    COMMENT '用于时间线回源查询优化，按状态过滤后按发布时间和ID降序排序';
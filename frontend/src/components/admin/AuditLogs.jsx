import React, { useEffect, useState } from 'react';
import { Table } from 'antd';
import apiClient from '../../services/apiClient';

const columns = [
  { title: 'ID', dataIndex: 'id' },
  { title: 'Admin User', dataIndex: 'adminUserId' },
  { title: 'Action', dataIndex: 'action' },
  { title: 'Entity', dataIndex: 'targetEntity' },
  { title: 'Target ID', dataIndex: 'targetId' },
  { title: 'Timestamp', dataIndex: 'timestamp' }
];

export default function AuditLogs() {
  const [data, setData] = useState([]);
  useEffect(() => {
    apiClient.get('/api/admin/audit-logs').then(res => setData(res.data));
  }, []);
  return <Table rowKey="id" columns={columns} dataSource={data} />;
}

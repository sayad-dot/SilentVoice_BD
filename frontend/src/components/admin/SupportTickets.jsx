import React, { useEffect, useState } from 'react';
import { Table, Select, Tag } from 'antd';
import apiClient from '../../services/apiClient';

const { Option } = Select;
export default function SupportTickets() {
  const [tickets, setTickets] = useState([]);
  useEffect(() => {
    apiClient.get('/api/admin/tickets').then(r => setTickets(r.data));
  }, []);

  const updateStatus = (id, status) => {
    apiClient
      .patch(`/api/admin/tickets/${id}?status=${status}`)
      .then(res =>
        setTickets(ts => ts.map(t => (t.id === id ? res.data : t)))
      );
  };

  const columns = [
    { title: 'ID', dataIndex: 'id' },
    { title: 'User ID', dataIndex: 'userId' },
    { title: 'Category', dataIndex: 'category' },
    { title: 'Message', dataIndex: 'message' },
    {
      title: 'Status',
      dataIndex: 'status',
      render: (_, row) => (
        <Select
          value={row.status}
          onChange={val => updateStatus(row.id, val)}
        >
          <Option value="OPEN">OPEN</Option>
          <Option value="IN_PROGRESS">IN_PROGRESS</Option>
          <Option value="RESOLVED">RESOLVED</Option>
        </Select>
      )
    },
    {
      title: 'Updated At',
      dataIndex: 'updatedAt',
      render: dt => new Date(dt).toLocaleString()
    }
  ];

  return <Table rowKey="id" columns={columns} dataSource={tickets} />;
}

import React, { useEffect, useState } from 'react';
import { Table, Switch } from 'antd';
import apiClient from '../../services/apiClient';

export default function FeatureFlags() {
  const [flags, setFlags] = useState([]);
  useEffect(() => {
    apiClient.get('/api/admin/feature-flags').then(r => setFlags(r.data));
  }, []);

  const toggle = (id, checked) => {
    apiClient
      .patch(`/api/admin/feature-flags/${id}?enabled=${checked}`)
      .then(res =>
        setFlags(fs => fs.map(f => (f.id === id ? res.data : f)))
      );
  };

  const cols = [
    { title: 'Key', dataIndex: 'flagKey' },
    { title: 'Description', dataIndex: 'description' },
    {
      title: 'Enabled',
      dataIndex: 'enabled',
      render: (_, r) => (
        <Switch checked={r.enabled} onChange={v => toggle(r.id, v)} />
      )
    }
  ];

  return <Table rowKey="id" columns={cols} dataSource={flags} />;
}

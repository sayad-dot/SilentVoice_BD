import React from 'react';
import { Layout, Menu } from 'antd';
import {
  UserOutlined,
  ExceptionOutlined,
  ControlOutlined
} from '@ant-design/icons';
import './AdminLayout.css';

const { Sider, Content } = Layout;

export default function AdminLayout({ selectedKey, onSelect, children }) {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          onClick={({ key }) => onSelect(key)}
        >
          <Menu.Item key="audit" icon={<ExceptionOutlined />}>
            Audit Logs
          </Menu.Item>
          <Menu.Item key="tickets" icon={<UserOutlined />}>
            Support Tickets
          </Menu.Item>
          <Menu.Item key="flags" icon={<ControlOutlined />}>
            Feature Flags
          </Menu.Item>
        </Menu>
      </Sider>
      <Layout>
        <Content style={{ margin: '16px' }}>{children}</Content>
      </Layout>
    </Layout>
  );
}

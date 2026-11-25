import { Link } from 'react-router-dom';
import Header from '../components/Header';
import LocationForm from '../components/admin/LocationForm';
import UploadAndLog from '../components/admin/UploadAndLog';

export default function AdminPage() {
  return (
    <div className="page">
      <Header
        action={
          <Link className="button-link" to="/">
            ← Back to marketplace
          </Link>
        }
      />
      <main className="admin">
        <h2>Marketplace Admin</h2>
        <LocationForm />
        <UploadAndLog />
      </main>
    </div>
  );
}
